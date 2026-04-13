# Plan de Integración — CJ Dropshipping Shopping API

> **Servicios afectados:** `mic-orderservice` (principal) · `mic-productcategory` (extensión) · `mic-notificationservice` (tracking)
> **API CJ:** `https://developers.cjdropshipping.com/api2.0/v1`
> **Versiones usadas:** createOrderV3 (más reciente), getOrderDetail (con feature LOGISTICS_TIMELINESS), payBalanceV2 (más reciente)
> **Regla:** Si dos endpoints hacen lo mismo, se usa el más completo; si la información ya viene incluida en una llamada, no se hace otra.

---

## Resumen Ejecutivo

El flujo actual sólo gestiona el pedido **internamente** (cart → order → payment). Una vez que el pago se confirma via Kafka (`payment.confirmed`), el orden pasa a estado `CONFIRMED` pero nunca se crea en CJ. Este plan cierra ese gap: al confirmar el pago, `mic-orderservice` crea el pedido en CJ vía API, persiste el CJ Order ID y su estado, y a partir de ahí sincroniza el tracking periódicamente.

**Endpoints CJ seleccionados (por criterio de versión más completa):**

| Propósito | Endpoint elegido | Descartado | Razón |
|---|---|---|---|
| Crear pedido | `createOrderV3` | `createOrderV2` | V3 usa solo `payType` integrado (no separado), misma respuesta, más limpia |
| Pagar balance | `payBalanceV2` | `payBalance` | V2 usa `shipmentOrderId + payId` (más seguro y explícito) |
| Detalle pedido | `getOrderDetail` | `list order` | getOrderDetail devuelve todo incluyendo productList; list sólo para admin paginado |
| Agregar carrito | `addCart + addCartConfirm` | N/A | Solo se usa con `payType=2` (balance); flujo separado |
| Balance | `getBalance` | N/A | Solo admin dashboard |

**Endpoints CJ descartados por no aplicar a nuestro flujo:**
- `changeWarehouse` — solo logística de plataforma (shopLogisticsType=1); nosotros usamos seller logistics (tipo 2)
- `uploadWaybillInfo / updateWaybillInfo` — solo logística de plataforma (tipo 1)
- `podProductCustomPicturesEdit` — solo productos POD (personalización), no en nuestro catálogo
- `saveGenerateParentOrder` — complemento del flujo de carrito añadido (payType=2), no nuestro caso principal
- `deleteOrder` — solo pedidos en estado CREATED/CANCELLED; gestionado indirectamente via cancelación

---

## 1. Estado Actual

### 1.1 Flujo de orden existente

```
Frontend → POST /api/v1/orders → OrderUseCaseImpl.createFromCart()
     → Order guardado con status=DRAFT
     → confirmOrder() → status=PENDING, Kafka order.created
     → PaymentService consume → procesa Stripe/PayPal/Crypto
     → Kafka payment.confirmed → OrderEventConsumerService
     → orderUseCase.updateStatus(CONFIRMED)
     ⚠️ AQUÍ TERMINA HOY — nunca se crea el pedido en CJ
```

### 1.2 Campos en `Order` / `orders` table actuales relevantes

| Campo | Tipo | Uso actual |
|---|---|---|
| `id` | VARCHAR(64) | Nuestro order ID interno |
| `order_number` | VARCHAR(30) | Nuestro número de pedido (ej: `NX-20260411-1234`) |
| `status` | OrderStatus | DRAFT→PENDING→CONFIRMED→PROCESSING→SHIPPED→IN_TRANSIT→DELIVERED |
| `saga_status` | OrderSagaStatus | Orquestación entre payment/stock |
| `shipping_address` | JSONB | Dirección de envío |
| `currency_code` | VARCHAR(10) | Moneda del pedido (multi-moneda) |
| `exchange_rate_to_usd` | BigDecimal | Tasa de cambio para reconvertir a USD (necesario para CJ) |

### 1.3 OrderItem existente

| Campo | Notas |
|---|---|
| `variant_id` | `vid` de CJ — es lo que CJ necesita como `vid` en `products[]` |
| `quantity` | Cantidad |
| `sku` | CJ sku (actualmente `null`, hay que poblar) |

### 1.4 Componentes de CJ ya disponibles en mic-productcategory

- `CjDropshippingClient` — WebClient configurado, `cjTokenManager`, Resilience4j retry+CB
- `CjTokenManager` — gestiona token life-cycle automáticamente
- `CjApiResponseDto<T>` — wrapper genérico de respuesta CJ

### 1.5 Last DB changeset

- `mic-orderservice`: id=10 (db.changelog-2.0.sql)

---

## 2. Arquitectura de la Integración

### 2.1 Principio de diseño

- `mic-orderservice` orquesta la creación del pedido en CJ (ya tiene: shipping address, items, variant IDs, currencies).
- `mic-productcategory` **no** se toca para el flujo de compra; sigue siendo el catálogo.
- CJ usa **USD** internamente — el `shopAmount` que enviamos debe ser en USD (se usa `exchange_rate_to_usd` ya almacenado en el pedido).
- Modo de pago: usamos `payType=3` por defecto (crear pedido sin iniciar pago CJ), ya que nosotros procesamos el pago con Stripe/PayPal. CJ solo necesita el pedido para ejecutar el fulfillment.
- Modo de logística: `shopLogisticsType=2` (Seller Logistics, no se requiere `storageId`).

### 2.2 Nuevo componente: `CjShoppingClient` en mic-orderservice

Separado del `CjDropshippingClient` de mic-productcategory (diferentes dominios; no compartir infraestructura entre microservicios). `mic-orderservice` tiene su propia configuración de WebClient para CJ.

### 2.3 Flujo completo con CJ integrado

```
payment.confirmed (Kafka)
     │
     ▼
OrderEventConsumerService.onPaymentConfirmed()
     │
     ├── updateStatus(CONFIRMED) [ya existe]
     │
     └── [NUEVO] CjOrderFulfillmentService.submitOrderToCj(orderId)
               │
               ├── 1. Construye CjCreateOrderV3Request desde Order + OrderItems
               │      - orderNumber = order.orderNumber (nuestro ref externo)
               │      - shippingAddress fields ← order.shippingAddress (JSONB)
               │      - logisticName ← shipping_carriers.cj_logistic_name (nuevo campo)
               │      - fromCountryCode = "CN" (default CJ)
               │      - shopLogisticsType = 2 (Seller Logistics)
               │      - payType = 3 (solo crea pedido, sin pago CJ)
               │      - shopAmount = order.total * exchange_rate_to_usd (convertir a USD)
               │      - products[] ← OrderItems (vid + quantity + storeLineItemId)
               │
               ├── 2. POST /shopping/order/createOrderV3
               │      → Responde: orderId, shipmentOrderId, productInfoList
               │
               ├── 3. Persiste CjOrderEntity:
               │      cj_order_id, shipment_order_id, cj_order_status=CREATED
               │      product_info_list (JSONB)
               │
               └── 4. updateStatus(PROCESSING) [order → PROCESSING]
```

### 2.4 Sincronización de estado CJ → Nuestro sistema

```
@Scheduled(cron = "0 */15 * * * *")  ← cada 15 min
CjOrderStatusSyncScheduler
     │
     ├── Busca orders con: status IN (PROCESSING, SHIPPED, IN_TRANSIT)
     │                     AND cj_order_id IS NOT NULL
     │
     ├── GET /shopping/order/getOrderDetail?orderId={cj_order_id}&features=LOGISTICS_TIMELINESS
     │
     ├── Mapea CJ status → nuestro OrderStatus:
     │     CREATED/IN_CART/UNPAID → PROCESSING (no change expected)
     │     UNSHIPPED              → PROCESSING
     │     SHIPPED                → SHIPPED (+ registra tracking number)
     │     DELIVERED              → DELIVERED
     │     CANCELLED              → CANCELLED
     │
     ├── trackNumber + logisticName → TrackingEvent
     └── Publica Kafka order.status.updated si cambia
```

---

## 3. Cambios en mic-orderservice

### 3.1 DB Migration — db.changelog-2.1.sql (id=11)

```sql
-- ============================================================
-- CJ Order Integration: cj_orders + shipping carrier logistic name
-- ChangeSet id: 11
-- ============================================================

-- Tabla para persistir el estado del pedido en CJ
CREATE TABLE IF NOT EXISTS cj_orders (
    id                  VARCHAR(64) PRIMARY KEY,
    order_id            VARCHAR(64) NOT NULL UNIQUE REFERENCES orders(id),
    cj_order_id         VARCHAR(200) NOT NULL,
    shipment_order_id   VARCHAR(200),
    cj_order_status     VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    track_number        VARCHAR(200),
    logistic_name       VARCHAR(200),
    product_info_list   JSONB,
    last_synced_at      TIMESTAMP,
    error_count         INT NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(120),
    updated_by          VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_cj_orders_order_id       ON cj_orders(order_id);
CREATE INDEX IF NOT EXISTS idx_cj_orders_cj_order_id    ON cj_orders(cj_order_id);
CREATE INDEX IF NOT EXISTS idx_cj_orders_status         ON cj_orders(cj_order_status);

-- Campo para mapear nuestro carrier al nombre logístico de CJ
-- Ej: "CJPacket Ordinary", "PostNL", "DHL"
ALTER TABLE shipping_carriers
    ADD COLUMN IF NOT EXISTS cj_logistic_name VARCHAR(200);

-- Campo para persistir el tracking number directamente en orders (consulta rápida)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS track_number VARCHAR(200);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS cj_order_id VARCHAR(200);
```

### 3.2 Nuevas clases — Árbol de archivos

```
mic-orderservice/src/main/java/com/backandwhite/
├── domain/
│   ├── model/
│   │   └── CjOrder.java                          [NUEVO] dominio del pedido CJ
│   ├── repository/
│   │   └── CjOrderRepository.java                [NUEVO] puerto de repo
│   └── valueobject/
│       └── CjOrderStatus.java                    [NUEVO] enum (CREATED, IN_CART, UNPAID, UNSHIPPED, SHIPPED, DELIVERED, CANCELLED)
├── application/
│   ├── port/out/
│   │   └── CjShoppingPort.java                   [NUEVO] puerto de salida para CJ Shopping
│   ├── usecase/
│   │   └── CjOrderFulfillmentUseCase.java        [NUEVO] interfaz
│   ├── usecase/impl/
│   │   └── CjOrderFulfillmentUseCaseImpl.java    [NUEVO] orquestación
│   └── scheduler/
│       └── CjOrderStatusSyncScheduler.java       [NUEVO] sync estado cada 15 min
├── infrastructure/
│   ├── client/cj/
│   │   ├── CjShoppingClient.java                 [NUEVO] implementa CjShoppingPort
│   │   ├── CjShoppingTokenManager.java           [NUEVO] gestión de token CJ (igual que mic-productcategory pero independiente)
│   │   └── dto/
│   │       ├── CjCreateOrderV3RequestDto.java    [NUEVO]
│   │       ├── CjCreateOrderV3ResponseDto.java   [NUEVO]
│   │       ├── CjOrderDetailResponseDto.java     [NUEVO]
│   │       ├── CjOrderProductItemDto.java        [NUEVO]
│   │       ├── CjBalanceResponseDto.java         [NUEVO] (para admin dashboard)
│   │       └── CjProductInfoListItemDto.java     [NUEVO]
│   └── db/postgres/
│       ├── entity/
│       │   └── CjOrderEntity.java                [NUEVO] @SuperBuilder, extends AuditableEntity
│       ├── repository/
│       │   └── CjOrderJpaRepository.java         [NUEVO]
│       ├── repository/impl/
│       │   └── CjOrderRepositoryImpl.java        [NUEVO]
│       └── mapper/
│           └── CjOrderInfraMapper.java           [NUEVO] MapStruct
│
├── api/
│   ├── controller/
│   │   └── CjOrderAdminController.java           [NUEVO] endpoints admin para gestión manual
│   └── dto/out/
│       └── CjOrderDtoOut.java                    [NUEVO]
│
└── infrastructure/message/kafka/consumer/
    └── OrderEventConsumerService.java            [MODIFICAR] onPaymentConfirmed()
```

### 3.3 Detalle de cada componente nuevo

---

#### 3.3.1 `CjOrderStatus.java`

```java
public enum CjOrderStatus {
    CREATED, IN_CART, UNPAID, UNSHIPPED, SHIPPED, DELIVERED, CANCELLED;

    /** Mapea el estado CJ al estado interno de la plataforma */
    public OrderStatus toInternalStatus() {
        return switch (this) {
            case CREATED, IN_CART, UNPAID, UNSHIPPED -> OrderStatus.PROCESSING;
            case SHIPPED                              -> OrderStatus.SHIPPED;
            case DELIVERED                            -> OrderStatus.DELIVERED;
            case CANCELLED                            -> OrderStatus.CANCELLED;
        };
    }
}
```

---

#### 3.3.2 `CjOrder.java` (domain model)

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CjOrder {
    private String id;
    private String orderId;           // FK → nuestro Order.id
    private String cjOrderId;         // ID CJ (orderId en respuesta CJ)
    private String shipmentOrderId;   // shipmentOrderId en respuesta CJ
    private CjOrderStatus cjOrderStatus;
    private String trackNumber;
    private String logisticName;
    private List<Map<String, Object>> productInfoList; // JSON libre
    private Instant lastSyncedAt;
    private int errorCount;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

#### 3.3.3 `CjShoppingPort.java`

```java
public interface CjShoppingPort {
    CjCreateOrderV3ResponseDto createOrder(CjCreateOrderV3RequestDto request);
    CjOrderDetailResponseDto getOrderDetail(String cjOrderId);
    BigDecimal getBalance(); // para dashboard admin
}
```

---

#### 3.3.4 `CjCreateOrderV3RequestDto.java`

Mapea directamente la spec de la API:

```java
@Data @Builder
public class CjCreateOrderV3RequestDto {
    private String orderNumber;         // nuestro orderNumber
    private String shippingZip;
    private String shippingCountryCode; // 2 letras ISO
    private String shippingCountry;
    private String shippingProvince;
    private String shippingCity;
    private String shippingCounty;
    private String shippingPhone;
    private String shippingCustomerName;
    private String shippingAddress;
    private String shippingAddress2;
    private String houseNumber;
    private String email;
    private String taxId;
    private String remark;
    private String logisticName;        // de shipping_carriers.cj_logistic_name
    private String fromCountryCode;     // default "CN"
    private Integer shopLogisticsType;  // 2 (Seller Logistics)
    private Integer payType;            // 3 (create only)
    private BigDecimal shopAmount;      // total en USD
    private List<CjOrderProductItemDto> products;
}

@Data @Builder
public class CjOrderProductItemDto {
    private String vid;             // OrderItem.variantId
    private String sku;             // OrderItem.sku (fallback si vid null)
    private Integer quantity;       // OrderItem.quantity
    private BigDecimal unitPrice;   // OrderItem.unitPrice in USD
    private String storeLineItemId; // OrderItem.id (nuestro line item id)
}
```

---

#### 3.3.5 `CjShoppingClient.java`

Implementa `CjShoppingPort` usando `WebClient`. Reutiliza el mecanismo de token de CJ pero con instancia Resilience4j separada `cjShopping`.

```java
@Component @RequiredArgsConstructor @Log4j2
public class CjShoppingClient implements CjShoppingPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CB_NAME   = "cjShopping";

    private final WebClient cjWebClient;               // mismo bean de mic-orderservice config
    private final CjShoppingTokenManager tokenManager;

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME)
    public CjCreateOrderV3ResponseDto createOrder(CjCreateOrderV3RequestDto req) { ... }

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME)
    public CjOrderDetailResponseDto getOrderDetail(String cjOrderId) {
        // GET /shopping/order/getOrderDetail?orderId={cjOrderId}&features=LOGISTICS_TIMELINESS
        // NOTA: features=LOGISTICS_TIMELINESS para obtener tiempos estimados en la misma llamada
        // sin hacer otra llamada adicional
    }

    @Override
    public BigDecimal getBalance() {
        // GET /shopping/pay/getBalance
        // Retorna data.amount (USD)
    }
}
```

**Configuración Resilience4j para `cjShopping` en application.yaml:**
```yaml
resilience4j:
  retry:
    instances:
      cjShopping:
        max-attempts: 3
        wait-duration: 2s
        retry-exceptions:
          - org.springframework.web.reactive.function.client.WebClientException
  circuitbreaker:
    instances:
      cjShopping:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

---

#### 3.3.6 `CjOrderFulfillmentUseCaseImpl.java`

```java
@Service @RequiredArgsConstructor @Log4j2
@Transactional
public class CjOrderFulfillmentUseCaseImpl implements CjOrderFulfillmentUseCase {

    private final OrderRepository orderRepository;
    private final CjOrderRepository cjOrderRepository;
    private final CjShoppingPort cjShoppingPort;
    private final OrderUseCase orderUseCase;
    private final ShippingCarrierRepository shippingCarrierRepository;

    @Override
    public void submitOrderToCj(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(...);

        // 1. Construir request
        //    - Dirección de envío desde order.shippingAddress (JSONB Map)
        //    - Moneda: siempre USD para CJ → si currencyCode != USD,
        //      total * exchange_rate_to_usd
        BigDecimal shopAmountUsd = convertToUsd(order);

        //    - logisticName: si el carrier tiene cj_logistic_name configurado, usarlo
        //      si no, usar "Other" (CJ lo acepta como fallback)
        String logisticName = resolveLogisticName(order);

        List<CjOrderProductItemDto> products = order.getItems().stream()
            .map(item -> CjOrderProductItemDto.builder()
                .vid(item.getVariantId())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice().getAmount().multiply(order.getExchangeRateToUsd()))
                .storeLineItemId(item.getId())
                .build())
            .toList();

        CjCreateOrderV3RequestDto request = CjCreateOrderV3RequestDto.builder()
            .orderNumber(order.getOrderNumber())
            .shippingCountryCode(extractField(order.getShippingAddress(), "countryCode"))
            .shippingCountry(extractField(order.getShippingAddress(), "country"))
            .shippingProvince(extractField(order.getShippingAddress(), "region"))
            .shippingCity(extractField(order.getShippingAddress(), "city"))
            .shippingZip(extractField(order.getShippingAddress(), "zipCode"))
            .shippingPhone(extractField(order.getShippingAddress(), "phone"))
            .shippingCustomerName(extractField(order.getShippingAddress(), "fullName"))
            .shippingAddress(buildAddressLine1(order.getShippingAddress()))
            .email(extractField(order.getShippingAddress(), "email"))
            .logisticName(logisticName)
            .fromCountryCode("CN")
            .shopLogisticsType(2)          // Seller Logistics
            .payType(3)                    // Solo crear pedido, sin pago CJ
            .shopAmount(shopAmountUsd)
            .products(products)
            .build();

        // 2. Llamar CJ API
        try {
            CjCreateOrderV3ResponseDto response = cjShoppingPort.createOrder(request);

            // 3. Persistir CjOrder
            CjOrder cjOrder = CjOrder.builder()
                .orderId(orderId)
                .cjOrderId(response.getOrderId())
                .shipmentOrderId(response.getShipmentOrderId())
                .cjOrderStatus(CjOrderStatus.CREATED)
                .productInfoList(mapProductInfoList(response.getProductInfoList()))
                .build();
            cjOrderRepository.save(cjOrder);

            // 4. Actualizar campo cj_order_id en orders para consulta rápida
            orderRepository.updateCjOrderId(orderId, response.getOrderId());

            // 5. Transicionar a PROCESSING internamente
            // (no publicar Kafka aquí; se publicará en la próxima sincronización de estado)
            orderUseCase.updateStatus(orderId, OrderStatus.PROCESSING, "CJ", "Order submitted to CJ");

            log.info("::> CJ order created: internalOrderId={}, cjOrderId={}", orderId, response.getOrderId());

        } catch (Exception e) {
            // No cancelar el pedido: registrar error y reintentar en próximo ciclo
            log.error("::> Failed to submit order {} to CJ: {}", orderId, e.getMessage(), e);
            // Registrar en cj_orders con errorCount++, lastError
            // El scheduler de reintento lo volverá a intentar (ver 3.3.7)
        }
    }
}
```

**Política de reintentos para órdenes CJ fallidas:**
- Pedidos con `cj_order_id IS NULL` y `error_count < 5` se reintentan cada 30 min (`CjOrderRetryScheduler`)
- Al llegar a `error_count = 5`, se envía alerta al admin via Kafka → notificationservice

---

#### 3.3.7 `CjOrderStatusSyncScheduler.java`

```java
@Component @RequiredArgsConstructor @Log4j2
public class CjOrderStatusSyncScheduler {

    // Cron: cada 15 minutos
    @Scheduled(cron = "0 */15 * * * *")
    public void syncOrderStatuses() {
        // 1. Busca CjOrders con status != DELIVERED && status != CANCELLED
        //    y que tengan cjOrderId (ya creados en CJ)
        // 2. Por cada uno: GET /shopping/order/getOrderDetail?orderId={cjOrderId}&features=LOGISTICS_TIMELINESS
        // 3. Si cambió:
        //    a. Actualiza cj_orders.cj_order_status
        //    b. Si trackNumber cambió: actualiza orders.track_number + inserta TrackingEvent
        //    c. Actualiza OrderStatus interno si mapped status cambió
        //    d. Publica Kafka order.status.updated si status cambió
        //    e. Si SHIPPED: publica Kafka order.shipped (para notificationservice → email tracking)
    }
}
```

**Rate limiting:** procesar en lotes de 20 pedidos por ciclo con pausa de 500ms entre lotes (respeta ~2 req/s del plan Plus de CJ). Usar mismo patrón que CJ sync en mic-productcategory.

---

#### 3.3.8 `CjOrderRetryScheduler.java`

```java
@Component @RequiredArgsConstructor @Log4j2
public class CjOrderRetryScheduler {

    // Reintentar órdenes que fallaron al submitir a CJ
    // Cron: cada 30 minutos
    @Scheduled(cron = "0 */30 * * * *")
    public void retryFailedOrders() {
        // Busca órdenes con status=CONFIRMED, cj_order_id IS NULL,
        // error_count < 5, created_at > NOW() - INTERVAL '24 hours'
        // Por cada una: cjOrderFulfillmentUseCase.submitOrderToCj(orderId)
    }
}
```

---

#### 3.3.9 `CjOrderAdminController.java`

```java
@RestController
@RequestMapping("/api/v1/cj/orders")
public class CjOrderAdminController {

    // GET /api/v1/cj/orders/{orderId} → detalle de CjOrder
    @GetMapping("/{orderId}")
    @NxAdmin
    public ResponseEntity<CjOrderDtoOut> getCjOrder(@PathVariable String orderId, ...) { ... }

    // POST /api/v1/cj/orders/{orderId}/retry → reintentar submit a CJ
    @PostMapping("/{orderId}/retry")
    @NxAdmin
    public ResponseEntity<CjOrderDtoOut> retrySubmit(@PathVariable String orderId,
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth) { ... }

    // POST /api/v1/cj/orders/{orderId}/sync → forzar sync de estado desde CJ
    @PostMapping("/{orderId}/sync")
    @NxAdmin
    public ResponseEntity<CjOrderDtoOut> syncStatus(@PathVariable String orderId,
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth) { ... }

    // GET /api/v1/cj/balance → saldo CJ para admin dashboard
    @GetMapping("/balance")
    @NxAdmin
    public ResponseEntity<Map<String, BigDecimal>> getBalance(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth) { ... }
}
```

---

#### 3.3.10 Modificación `OrderEventConsumerService.java`

```java
// Línea a agregar DESPUÉS de updateStatus(orderId, OrderStatus.CONFIRMED, ...)
try {
    cjOrderFulfillmentUseCase.submitOrderToCj(orderId);
} catch (Exception e) {
    // No propagar: el fallo de CJ no debe revertir la confirmación interna
    log.error("::> CJ submit failed for order={}: {}", orderId, e.getMessage(), e);
}
```

---

#### 3.3.11 Modificación `OrderEntity.java` + `Order.java`

Agregar campos:
```java
// OrderEntity.java
@Column(name = "track_number", length = 200)
private String trackNumber;

@Column(name = "cj_order_id", length = 200)
private String cjOrderId;
```

```java
// Order.java
private String trackNumber;
private String cjOrderId;
```

---

### 3.4 Multi-moneda en CJ

CJ siempre opera en USD. Los pasos de conversión son:

1. `order.total` está en `order.currencyCode` (ej: EUR)
2. `order.exchangeRateToUsd` ya está persistido (inverso de la tasa de compra)
3. `shopAmount = order.total.getAmount() * order.exchangeRateToUsd`
4. `OrderItem.unitPrice` (en currency local) → USD = `unitPrice.getAmount() * exchangeRateToUsd`

Si `currencyCode = "USD"`, `exchangeRateToUsd = 1.0` → sin conversión.

---

### 3.5 Mapping de shipping address (JSONB → campos CJ)

La dirección en `order.shipping_address` (JSONB) tiene esta estructura actual:
```json
{
  "fullName": "John Doe",
  "phone": "+1-555-0100",
  "email": "john@example.com",
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4B",
  "city": "New York",
  "region": "NY",
  "zipCode": "10001",
  "country": "United States",
  "countryCode": "US"
}
```

Mapeo a CJ V3:

| Campo JSONB | Campo CJ | Notas |
|---|---|---|
| `fullName` | `shippingCustomerName` | |
| `phone` | `shippingPhone` | |
| `email` | `email` | |
| `addressLine1` | `shippingAddress` | |
| `addressLine2` | `shippingAddress2` | nullable |
| `city` | `shippingCity` | |
| `region` | `shippingProvince` | |
| `zipCode` | `shippingZip` | |
| `country` | `shippingCountry` | nombre completo |
| `countryCode` | `shippingCountryCode` | **2 letras ISO — requerido** |

> **Acción requerida frontend:** Asegurarse que el frontend envíe `countryCode` de 2 letras en el address (ISO 3166-1 alpha-2). Si actualmente no se envía, agregar dropdown de país con código.

---

### 3.6 Logística CJ — Resolución de `logisticName`

CJ requiere `logisticName` (ej: `"CJPacket Ordinary"`, `"PostNL"`, `"DHL"`). Fuente en nuestro sistema: `shipping_carriers.cj_logistic_name` (campo nuevo en migración 2.1).

**Estrategia de resolución:**
1. Si la orden tiene asociado un `carrier_id` (campo a agregar en `orders`), usar `shipping_carriers.cj_logistic_name`.
2. Si no hay carrier o el carrier no tiene `cj_logistic_name`, usar un valor por defecto configurable:
   ```yaml
   app:
     cj:
       default-logistic-name: ${CJ_DEFAULT_LOGISTIC_NAME:CJPacket Ordinary}
       from-country-code: ${CJ_FROM_COUNTRY_CODE:CN}
   ```

**Admin setup requerido:** Configurar `cj_logistic_name` en cada `ShippingCarrier` vía una pantalla admin (frontend) o directamente en DB. Ver sección 6 (Frontend).

---

### 3.7 Configuración de CJ en mic-orderservice

**Nuevo bean `WebClient` para CJ en `mic-orderservice`:**

```yaml
# application.yaml (mic-orderservice)
cj:
  api:
    base-url: ${CJ_API_BASE_URL:https://developers.cjdropshipping.com/api2.0/v1}
    email: ${CJ_API_EMAIL}
    password: ${CJ_API_PASSWORD}

app:
  cj:
    default-logistic-name: ${CJ_DEFAULT_LOGISTIC_NAME:CJPacket Ordinary}
    from-country-code: ${CJ_FROM_COUNTRY_CODE:CN}
    status-sync-cron: ${CJ_STATUS_SYNC_CRON:0 */15 * * * *}
    retry-cron: ${CJ_RETRY_CRON:0 */30 * * * *}
    sync-batch-size: ${CJ_SYNC_BATCH_SIZE:20}

resilience4j:
  retry:
    instances:
      cjShopping:
        max-attempts: 3
        wait-duration: 2s
  circuitbreaker:
    instances:
      cjShopping:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

---

## 4. Cambios en mic-notificationservice

### 4.1 Nuevo evento Kafka: `order.shipped` con tracking

Cuando `CjOrderStatusSyncScheduler` detecta `SHIPPED` y obtiene `trackNumber`:
- Publica `OrderShippedEvent` (nuevo Avro schema) o extiende el existente `ShippingOrderShippedEvent`
- `mic-notificationservice` consume y envía email de tracking al cliente

**Nuevo topic Kafka (en `AppConstants.java` del `app-common`):**
```java
public static final String KAFKA_TOPIC_CJ_ORDER_SUBMITTED  = "cj.order.submitted";
public static final String KAFKA_TOPIC_CJ_ORDER_SHIPPED    = "cj.order.shipped";
public static final String KAFKA_TOPIC_CJ_ORDER_FAILED     = "cj.order.failed";
```

### 4.2 Plantilla email de tracking (ya existe infraestructura Thymeleaf)

Añadir `order-tracking.html` con:
- Número de tracking
- Nombre de transporte logístico
- Enlace de seguimiento (si CJ lo provee; el campo `trackingUrl` en el detalle del pedido)
- Estado del pedido

---

## 5. Cambios en mic-productcategory

No se requieren cambios para el flujo de compra. La integración CJ Shopping se mantiene completamente en `mic-orderservice`.

**Único punto de contacto:** `OrderEventConsumerService` en mic-orderservice ya usa `CatalogPort` (que apunta a mic-productcategory) para verificar precios y stock antes de crear el pedido. Esto no cambia.

---

## 6. Cambios en el Frontend (mic-ecomerce)

### 6.1 Datos de envío (checkout form)

**Cambio requerido:** El formulario de checkout debe enviar `countryCode` (2 letras ISO) en `shippingAddress`, ya que CJ lo requiere como campo obligatorio.

**Archivos a modificar:**
- `src/app/components/checkout/AddressForm.tsx` (o equivalente) — agregar campo oculto `countryCode` o dropdown de países con código
- Tipo `ShippingAddress` en `src/app/types/` — agregar campo `countryCode: string`
- Repository que llama `POST /api/v1/orders` — incluir `countryCode` en el payload

**Si ya existe un selector de país:** asegurarse de que guarda el código ISO-2 además del nombre.

### 6.2 Tracking en página de detalle de pedido

**La información de tracking (número + carrier) ya se almacena en `orders.track_number`** una vez llegue desde CJ. El frontend puede mostrarlo en el endpoint existente `GET /api/v1/orders/{id}` si se expone en `OrderDtoOut`.

**Modificaciones en `OrderDtoOut.java`:**
```java
private String trackNumber;   // nuevo campo
private String cjOrderId;     // opcional, solo para admin
```

**Frontend:** `src/app/pages/orders/OrderDetail.tsx` (o equivalente) — mostrar sección de tracking cuando `trackNumber != null`:
```tsx
{order.trackNumber && (
  <TrackingSection
    trackNumber={order.trackNumber}
    carrier={order.logisticName}
    status={order.status}
  />
)}
```

### 6.3 Admin: Panel de órdenes CJ

**Nueva pestaña en admin de órdenes:** Estado CJ, track number, botones "Retry" y "Sync Now".

**Endpoint que consume:** `GET /api/v1/cj/orders/{orderId}`, `POST /api/v1/cj/orders/{orderId}/retry`, `POST /api/v1/cj/orders/{orderId}/sync`

### 6.4 Admin: Configuración de logísticas CJ

**Nueva sección en admin de carriers de envío:** Campo `cjLogisticName` editable por carrier para mapear nuestros carriers al nombre de logística que acepta CJ.

---

## 7. Multi-idioma

El flujo de Shopping con CJ **no genera contenido traducible** — los datos son estructurados (estados, números, fechas). Los únicos textos que se muestran al cliente son:
- **Estado del pedido**: ya traducido en frontend (`OrderStatus` → label i18n)
- **Emails de tracking**: plantillas Thymeleaf en notificationservice — crear versiones en `es`, `en`, `pt-BR` usando las plantillas existentes como base

**No se requiere integración i18n nueva** para esta fase de Shopping.

---

## 8. Avro Schemas requeridos (app-kafka en mic-coreservice)

```
app-kafka/src/main/avro/
├── cj_order_submitted_event.avsc    [NUEVO]
├── cj_order_shipped_event.avsc      [NUEVO]
└── cj_order_failed_event.avsc       [NUEVO]
```

**`cj_order_submitted_event.avsc`:**
```json
{
  "type": "record",
  "name": "CjOrderSubmittedEvent",
  "namespace": "com.backandwhite.core.kafka.avro",
  "fields": [
    {"name": "orderId",         "type": "string"},
    {"name": "cjOrderId",       "type": "string"},
    {"name": "shipmentOrderId", "type": ["null", "string"], "default": null},
    {"name": "userId",          "type": "string"},
    {"name": "timestamp",       "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**`cj_order_shipped_event.avsc`:**
```json
{
  "type": "record",
  "name": "CjOrderShippedEvent",
  "namespace": "com.backandwhite.core.kafka.avro",
  "fields": [
    {"name": "orderId",       "type": "string"},
    {"name": "cjOrderId",     "type": "string"},
    {"name": "userId",        "type": "string"},
    {"name": "email",         "type": ["null", "string"], "default": null},
    {"name": "trackNumber",   "type": ["null", "string"], "default": null},
    {"name": "logisticName",  "type": ["null", "string"], "default": null},
    {"name": "trackingUrl",   "type": ["null", "string"], "default": null},
    {"name": "orderReference","type": "string"},
    {"name": "timestamp",     "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

**`cj_order_failed_event.avsc`:**
```json
{
  "type": "record",
  "name": "CjOrderFailedEvent",
  "namespace": "com.backandwhite.core.kafka.avro",
  "fields": [
    {"name": "orderId",    "type": "string"},
    {"name": "errorCode",  "type": ["null", "string"], "default": null},
    {"name": "errorMsg",   "type": "string"},
    {"name": "errorCount", "type": "int"},
    {"name": "timestamp",  "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

---

## 9. Flujos especiales — fuera del alcance de este plan pero documentados

### 9.1 Pago con balance CJ (`payBalanceV2`)

Este flujo solo aplica si se quiere pagar el fulfillment usando el saldo CJ en lugar de la facturación mensual.

**Flujo (uso eventual):**
1. `createOrderV3` con `payType=3` (ya implementado)
2. `addCart` → `POST /shopping/order/addCart` con `cjOrderIdList`
3. `addCartConfirm` → `POST /shopping/order/addCartConfirm`
4. `saveGenerateParentOrder` → obtiene `payId`
5. `payBalanceV2` con `shipmentOrderId + payId`

**Decisión:** No se implementa en esta fase. CJ factura mediante saldo de cuenta o CJ Pay. El `payType=3` es suficiente para el fulfillment; CJ procesa internamente el cobro al vendedor según contrato.

### 9.2 Cancelación de pedido en CJ

- CJ solo permite `deleteOrder` en estado `CREATED` o `CANCELLED`.
- Al cancelar un pedido internamente: si existe `cj_order_id` y `cjOrderStatus = CREATED`, llamar `DELETE /shopping/order/deleteOrder?orderId={cjOrderId}`.
- Agregar en `OrderUseCase.cancel()` esta lógica.

### 9.3 IOSS (pedidos a la UE > €150)

- Por defecto: `iossType` no se envía → null → CJ usa su configuración por defecto.
- Si en el futuro se requiere IOSS: agregar campo `iossType` en `Order` + lógica de aplicación basada en `shippingCountryCode` (países UE) y monto del pedido.

---

## 10. Checklist de Implementación

### Fase 1 — Backend Core (mic-orderservice)
- [ ] 10.1 — `db.changelog-2.1.sql` — tabla `cj_orders`, columnas nuevas en `orders` y `shipping_carriers`
- [ ] 10.2 — `db.changelog-master.yaml` — añadir changeSet id=11
- [ ] 10.3 — `CjOrderStatus.java` — enum con `toInternalStatus()`
- [ ] 10.4 — `CjOrder.java` — domain model
- [ ] 10.5 — `CjOrderRepository.java` — interfaz: `save`, `findByOrderId`, `findByCjOrderId`, `findPendingSync(int limit)`, `findFailedToSubmit(int limit)`, `update`
- [ ] 10.6 — `CjShoppingPort.java` — interfaz de puerto
- [ ] 10.7 — `CjCreateOrderV3RequestDto.java` + `CjOrderProductItemDto.java`
- [ ] 10.8 — `CjCreateOrderV3ResponseDto.java` + `CjOrderDetailResponseDto.java` + `CjProductInfoListItemDto.java` + `CjBalanceResponseDto.java`
- [ ] 10.9 — `CjShoppingTokenManager.java` — clonar lógica de `mic-productcategory/CjTokenManager` (mismo API pero instancia propia)
- [ ] 10.10 — Configurar bean `WebClient cjWebClient` en `mic-orderservice` (si no existe ya)
- [ ] 10.11 — `CjShoppingClient.java` — implementa `CjShoppingPort`
- [ ] 10.12 — `CjOrderEntity.java` — entidad JPA `@SuperBuilder extends AuditableEntity`
- [ ] 10.13 — `CjOrderJpaRepository.java` — JPA repo con queries JPQL
- [ ] 10.14 — `CjOrderInfraMapper.java` — MapStruct
- [ ] 10.15 — `CjOrderRepositoryImpl.java` — `implements CjOrderRepository`
- [ ] 10.16 — Extender `Order.java` + `OrderEntity.java` con `trackNumber` y `cjOrderId`
- [ ] 10.17 — Extender `OrderRepository` + `OrderRepositoryImpl` con `updateCjOrderId()`
- [ ] 10.18 — Extender `ShippingCarrier.java` / `ShippingCarrierEntity.java` con `cjLogisticName`
- [ ] 10.19 — `CjOrderFulfillmentUseCase.java` — interfaz
- [ ] 10.20 — `CjOrderFulfillmentUseCaseImpl.java` — lógica de submit + conversión de moneda + mapeo de dirección
- [ ] 10.21 — `CjOrderStatusSyncScheduler.java` — sync cada 15 min en lotes de 20
- [ ] 10.22 — `CjOrderRetryScheduler.java` — reintentar submit fallido cada 30 min
- [ ] 10.23 — `CjOrderAdminController.java` — endpoints admin
- [ ] 10.24 — `CjOrderDtoOut.java`
- [ ] 10.25 — Modificar `OrderEventConsumerService.onPaymentConfirmed()` → llamar `submitOrderToCj`
- [ ] 10.26 — Extender `OrderDtoOut.java` con `trackNumber`
- [ ] 10.27 — `application.yaml` / `application-local.yaml` / `application-dev.yaml` → añadir config CJ + Resilience4j

### Fase 2 — Kafka + Notificaciones (mic-coreservice + mic-notificationservice)
- [ ] 10.28 — Avro schemas: `cj_order_submitted_event.avsc`, `cj_order_shipped_event.avsc`, `cj_order_failed_event.avsc`
- [ ] 10.29 — Regenerar clases Avro (`mvn generate-sources` en `app-kafka`)
- [ ] 10.30 — `AppConstants.java` — añadir nuevas constantes de topics
- [ ] 10.31 — Añadir `publishCjOrderSubmitted()` y `publishCjOrderShipped()` y `publishCjOrderFailed()` a `OrderEventPort` + implementación en `KafkaOrderEventAdapter`
- [ ] 10.32 — `mic-notificationservice`: consumer para `cj.order.shipped` → email tracking
- [ ] 10.33 — Plantilla Thymeleaf `order-tracking.html` en 3 idiomas (en, es, pt-BR)

### Fase 3 — Frontend
- [ ] 10.34 — Verificar/añadir `countryCode` (ISO 2) en formulario de checkout
- [ ] 10.35 — Extender tipo `ShippingAddress` con `countryCode`
- [ ] 10.36 — `OrderDetail.tsx` — mostrar sección tracking cuando `trackNumber != null`
- [ ] 10.37 — Admin Órdenes: nueva columna/tab con estado CJ + tracking number + botones Retry/Sync
- [ ] 10.38 — Admin Carriers: campo `cjLogisticName` editable
- [ ] 10.39 — Repository frontend para endpoints `GET/POST /api/v1/cj/orders/...`

### Fase 4 — Configuración y Datos
- [ ] 10.40 — Variables de entorno: `CJ_API_EMAIL`, `CJ_API_PASSWORD` en `docker-compose.yml` de mic-orderservice
- [ ] 10.41 — Configurar `cj_logistic_name` en shipping_carriers (al menos uno por zona geográfica principal)
- [ ] 10.42 — Añadir `@EnableScheduling` en `MicOrderserviceApplication.java` si no está activo

---

## 11. Diagrama de Secuencia Completo

```
Frontend                 mic-orderservice              CJ API            mic-notificationservice
   │                           │                          │                       │
   ├─ POST /checkout ─────────►│                          │                       │
   │  (shippingAddress         │                          │                       │
   │   + countryCode)          │ createFromCart()         │                       │
   │                           ├─ [valida precios/stock]  │                       │
   │                           │   ←mic-productcategory   │                       │
   │                           │ save(order DRAFT)        │                       │
   │ ◄─ orderId ───────────────┤                          │                       │
   │                           │                          │                       │
   ├─ POST /payment ──────────►│ (mic-paymentservice)     │                       │
   │                           │  ←Kafka payment.confirmed│                       │
   │                           │ updateStatus(CONFIRMED)  │                       │
   │                           │ submitOrderToCj() ───────►                       │
   │                           │                          │ POST /createOrderV3   │
   │                           │  ◄─ cjOrderId ──────────┤                       │
   │                           │ save(CjOrder{cjOrderId}) │                       │
   │                           │ updateStatus(PROCESSING) │                       │
   │                           │                          │                       │
   │ [15 min later]            │                          │                       │
   │                           │ scheduledSync() ─────────►                      │
   │                           │                          │ GET /getOrderDetail   │
   │                           │  ◄─ status=SHIPPED ──────┤  +LOGISTICS_TIMELINESS│
   │                           │    trackNumber=XYZ        │                      │
   │                           │ updateStatus(SHIPPED)    │                       │
   │                           │ save(trackNumber)        │                       │
   │                           │ Kafka cj.order.shipped ──┼──────────────────────►│
   │                           │                          │                       │ send tracking email
```

---

## 12. Decisiones Técnicas Documentadas

| # | Decisión | Alternativa descartada | Razón |
|---|---|---|---|
| D1 | `payType=3` (crear sin pago CJ) | `payType=2` (balance CJ) | Procesamos pago internamente; CJ solo hace fulfillment |
| D2 | `shopLogisticsType=2` (Seller Logistics) | `shopLogisticsType=1 o 3` | No necesitamos warehouse CJ; usamos sus carriers desde China |
| D3 | `createOrderV3` sobre `createOrderV2` | V2 | V3 es más reciente (misma respuesta, interfaz más limpia) |
| D4 | `getOrderDetail + LOGISTICS_TIMELINESS` para sync | Separar en dos llamadas | Una sola llamada trae todo; ahorra rate limit |
| D5 | CjShoppingClient independiente en mic-orderservice | Compartir con mic-productcategory | Principio de microservicios: cada servicio gestiona su infraestructura |
| D6 | Submit asíncrono post-payment (no bloquea confirmación) | Sync en el mismo request | Fallo de CJ nunca debe cancelar un pago legítimo |
| D7 | Reintentos con scheduler cada 30 min (max 5) | Reintentar infinitamente | Evita loops; escala a alerta admin si persiste |
| D8 | `shopAmount` en USD | Enviar en moneda local | CJ no acepta multi-moneda; ya tenemos `exchange_rate_to_usd` en `orders` |
