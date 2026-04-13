# PLAN: CJ Dropshipping — Flujo End-to-End (Token → Envío al Cliente)

> **Objetivo:** Completar el ciclo de vida completo de un pedido dropshipping desde la
> autenticación hasta la entrega al cliente final, integrando SOLO lo que falta sobre
> la base ya implementada.
>
> **Microservicios involucrados:** `mic-orderservice` (principal), `mic-productcategory` (ya completo), `mic-notificationservice`
>
> **Referencia API:** https://developers.cjdropshipping.cn/en/api/api2/

---

## Estado Actual de la Integración

### ✅ YA IMPLEMENTADO (No tocar)

| Componente | Endpoint CJ | Ubicación |
|---|---|---|
| Autenticación | `POST /authentication/getAccessToken` | `mic-productcategory` + `mic-orderservice` |
| Refresh token | `POST /authentication/refreshAccessToken` | Ambos servicios (30 min antes de expirar) |
| Categorías | `GET /product/getCategory` | `mic-productcategory` |
| Listado productos | `GET /product/listV2` | `mic-productcategory` (sync diario 3 AM) |
| Detalle producto | `GET /product/query` | `mic-productcategory` |
| Inventario | `GET /product/stock/getInventoryByPid` | `mic-productcategory` (cada 4 horas) |
| Reviews | `GET /product/productComments` | `mic-productcategory` (diario 5 AM) |
| Discovery Crawler | Multi-estrategia (BY_CATEGORY, BY_KEYWORD, BY_TIME) | `mic-productcategory` |
| Crear orden CJ | `POST /shopping/order/createOrderV3` (payType=3) | `mic-orderservice` |
| Detalle orden CJ | `GET /shopping/order/getOrderDetail` | `mic-orderservice` (cada 15 min) |
| Balance cuenta | `GET /shopping/pay/getBalance` | `mic-orderservice` (admin) |
| Retry automático | Reintento cada 30 min para órdenes fallidas | `mic-orderservice` |
| Resiliencia | Resilience4j (retry 3x, circuit breaker, timeout 30s) | Ambos servicios |

### ❌ FALTA POR IMPLEMENTAR

| Componente | Endpoint CJ | Prioridad |
|---|---|---|
| Cálculo de flete | `POST /logistic/freightCalculate` | ALTA — Necesario pre-checkout |
| Flujo pago CJ completo | addCart → addCartConfirm → saveGenerateParentOrder → payBalanceV2 | ALTA — Sin esto CJ no procesa |
| Webhooks CJ | `POST /webhook/set` + Receiver endpoints | ALTA — Reemplaza polling 15 min |
| Tracking detallado | `GET /logistic/trackInfo` | MEDIA — Tracking para cliente |
| Cancelar orden CJ | `DELETE /shopping/order/deleteOrder` | MEDIA — Cancelaciones |
| Listar órdenes CJ | `GET /shopping/order/list` | BAJA — Solo admin |

### ⛔ EXCLUIDO DEL PLAN (No aplica a nuestro flujo)

| Componente | Razón |
|---|---|
| OAuth (getAuthorizeUrl, exchangeAccessToken) | Usamos apiKey directamente |
| getAffiliateAccessToken | No somos afiliados |
| Platform Logistics (shopLogisticsType=1) | Usamos Seller Logistics (type=2), CJ gestiona envío |
| Upload/Update Waybill Info | Solo para Platform Logistics |
| Change Order Warehouse | Solo para Platform Logistics |
| POD Pictures (podProductCustomPicturesEdit) | No tenemos productos POD |
| Settings API | No requerido |
| Dispute API | Fase futura (post-MVP) |
| Sourcing API | Fase futura |
| Partner Freight Calculation | Para partners, no aplica |
| Supplier Logistics Template | Para proveedores, no aplica |

---

## Flujo Completo: Token → Envío al Cliente

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    FLUJO END-TO-END DROPSHIPPING                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [YA IMPLEMENTADO]                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐               │
│  │ 1. Token     │───>│ 2. Sync      │───>│ 3. Cliente   │               │
│  │ getAccessToken│    │ Productos    │    │ Navega/Busca │               │
│  │ (apiKey)     │    │ listV2+query │    │ Catálogo     │               │
│  └──────────────┘    └──────────────┘    └──────┬───────┘               │
│                                                  │                      │
│  [NUEVO - FASE 1]                                ▼                      │
│  ┌──────────────────────────────────────────────────────┐               │
│  │ 4. Calcular Flete (freightCalculate)                 │               │
│  │    - Cliente selecciona productos + dirección        │               │
│  │    - Mostrar opciones de envío + costos + tiempos    │               │
│  │    - Cliente elige carrier (logisticName)            │               │
│  └──────────────────────┬───────────────────────────────┘               │
│                         │                                               │
│  [YA IMPLEMENTADO]      ▼                                               │
│  ┌──────────────────────────────────────────────────────┐               │
│  │ 5. Cliente hace Checkout → Pago (Stripe/PayPal)      │               │
│  │    → Kafka: payment.confirmed                        │               │
│  │    → createOrderV3(payType=3) → CJ Order creada     │               │
│  └──────────────────────┬───────────────────────────────┘               │
│                         │                                               │
│  [NUEVO - FASE 2]       ▼                                               │
│  ┌──────────────────────────────────────────────────────┐               │
│  │ 6. Procesar Pago en CJ (completar el ciclo)          │               │
│  │    a) addCart(cjOrderId)                              │               │
│  │    b) addCartConfirm(cjOrderId)                      │               │
│  │    c) saveGenerateParentOrder(shipmentOrderId)       │               │
│  │    d) payBalanceV2(shipmentOrderId, payId)           │               │
│  │    → Order status: CREATED → IN_CART → UNPAID → UNSHIPPED           │
│  └──────────────────────┬───────────────────────────────┘               │
│                         │                                               │
│  [NUEVO - FASE 3]       ▼                                               │
│  ┌──────────────────────────────────────────────────────┐               │
│  │ 7. Webhooks CJ (notificaciones en tiempo real)       │               │
│  │    - ORDER: cambios de estado                        │               │
│  │    - LOGISTIC: tracking updates                      │               │
│  │    - PRODUCT/STOCK: cambios en catálogo/inventario   │               │
│  └──────────────────────┬───────────────────────────────┘               │
│                         │                                               │
│  [NUEVO - FASE 4]       ▼                                               │
│  ┌──────────────────────────────────────────────────────┐               │
│  │ 8. Tracking para el Cliente                          │               │
│  │    - trackInfo: eventos detallados de logística      │               │
│  │    - Notificaciones email: enviado, en tránsito,     │               │
│  │      entregado                                       │               │
│  └──────────────────────────────────────────────────────┘               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## FASE 1: Cálculo de Flete Pre-Checkout

> **Microservicio:** `mic-orderservice`
> **Endpoint CJ:** `POST /logistic/freightCalculate`
> **Propósito:** Antes de que el cliente complete el checkout, mostrar opciones de envío
> con costos y tiempos estimados de entrega.

### Endpoint CJ: freightCalculate

```
POST https://developers.cjdropshipping.com/api2.0/v1/logistic/freightCalculate
Header: CJ-Access-Token: xxx

Request:
{
  "startCountryCode": "CN",
  "endCountryCode": "US",
  "products": [
    { "quantity": 2, "vid": "variant-id-here" }
  ]
}

Response:
{
  "data": [
    {
      "logisticName": "USPS+",        // ← Nombre para usar en createOrderV3
      "logisticPrice": 4.71,           // USD
      "logisticPriceCn": 30.54,        // CNY
      "logisticAging": "2-5"           // Días estimados
    }
  ]
}
```

### Checklist Fase 1

- [ ] **F1.1** Crear `FreightCalculationPort.java` en dominio (interfaz puerto de salida)
  ```java
  public interface FreightCalculationPort {
      List<FreightOptionDto> calculateFreight(String fromCountry, String toCountry,
                                              List<FreightProductItem> products);
  }
  ```

- [ ] **F1.2** Crear DTOs: `FreightOptionDto` (logisticName, logisticPrice, logisticAging), `FreightProductItem` (vid, quantity)

- [ ] **F1.3** Implementar `CjFreightCalculationAdapter.java` en infraestructura — llama `POST /logistic/freightCalculate` via `CjShoppingClient`

- [ ] **F1.4** Agregar método `calculateFreight()` en `CjShoppingClient.java`:
  - URL: `/logistic/freightCalculate`
  - Método: POST
  - Headers: `CJ-Access-Token`
  - Resilience4j: retry 2x, timeout 10s (no circuit breaker — es consulta no crítica)

- [ ] **F1.5** Crear `FreightCalculationUseCaseImpl.java`:
  - Input: countryCode del shipping address + lista de variantIds con cantidades
  - fromCountryCode: usar `app.cj.from-country-code` (default CN)
  - Output: Lista de opciones de envío ordenadas por precio
  - Cachear resultado 30 minutos (misma combinación país+productos)

- [ ] **F1.6** Crear endpoint REST `GET /api/v1/orders/freight/calculate`:
  - Query params: `countryCode`, `products` (vid:quantity pares)
  - Response: lista de `{ logisticName, priceUsd, estimatedDays }`
  - Autorización: usuario autenticado

- [ ] **F1.7** Integrar con flujo de checkout: el `logisticName` seleccionado por el cliente se persiste en la orden y se envía en `createOrderV3`

- [ ] **F1.8** Tests unitarios para `FreightCalculationUseCaseImpl` y adapter

---

## FASE 2: Flujo Completo de Pago CJ (Fulfillment Pipeline)

> **Microservicio:** `mic-orderservice`
> **Propósito:** Después de crear la orden en CJ (payType=3), completar el ciclo:
> addCart → addCartConfirm → saveGenerateParentOrder → payBalanceV2
> para que CJ inicie el procesamiento y envío.
>
> **Flujo CJ documentado:** https://developers.cjdropshipping.cn/en/api/start/Orders-Synchronization-Processing.html

### Secuencia Detallada

```
Orden creada en CJ (createOrderV3, payType=3)
  → status: CREATED
  │
  ▼
addCart([cjOrderId])
  → status: IN_CART
  │
  ▼
addCartConfirm([cjOrderId])
  → Obtener shipmentOrderId
  │
  ▼
saveGenerateParentOrder(shipmentOrderId)
  → Obtener payId + paymentInformation (actualPayment, postage, etc.)
  │
  ▼
  CHECK: getBalance() >= actualPayment ?
  │  YES → payBalanceV2(shipmentOrderId, payId)
  │         → status: UNPAID → UNSHIPPED (CJ procesa y envía)
  │  NO  → Marcar como AWAITING_FUNDS, alerta admin
  │
  ▼
CJ procesa, empaca y envía
  → status: SHIPPED (con trackNumber)
  → status: DELIVERED
```

### Endpoints CJ necesarios

| Step | Endpoint | Método | Body |
|------|----------|--------|------|
| Add to Cart | `/shopping/order/addCart` | POST | `{ "cjOrderIdList": ["orderId"] }` |
| Confirm Cart | `/shopping/order/addCartConfirm` | POST | `{ "cjOrderIdList": ["orderId"] }` |
| Generate Parent | `/shopping/order/saveGenerateParentOrder` | POST | `{ "shipmentOrderId": "xxx" }` |
| Pay Balance V2 | `/shopping/pay/payBalanceV2` | POST | `{ "shipmentOrderId": "xxx", "payId": "xxx" }` |
| Get Balance | `/shopping/pay/getBalance` | GET | (sin body) |

### Checklist Fase 2

- [ ] **F2.1** Agregar métodos al `CjShoppingClient.java`:
  ```java
  Mono<CjResponse<AddCartResult>> addCart(List<String> cjOrderIds);
  Mono<CjResponse<AddCartConfirmResult>> addCartConfirm(List<String> cjOrderIds);
  Mono<CjResponse<GenerateParentOrderResult>> saveGenerateParentOrder(String shipmentOrderId);
  Mono<CjResponse<Void>> payBalanceV2(String shipmentOrderId, String payId);
  ```
  Cada uno con Resilience4j (retry 3x, circuit breaker, timeout 30s)

- [ ] **F2.2** Crear DTOs de respuesta:
  - `AddCartResult`: successCount, addSuccessOrders, interceptOrders
  - `AddCartConfirmResult`: submitSuccess, shipmentsId, interceptOrders
  - `GenerateParentOrderResult`: payId, orderMoney, payExpireTime, paymentInformation (actualPayment, balanceDeduction, freight, etc.), interceptOrders
  - `PayBalanceResult`: (vacío, solo validar code=200)

- [ ] **F2.3** Crear `CjFulfillmentPipelineService.java` — orquestador del flujo completo:
  ```java
  public CjFulfillmentResult processFulfillment(String cjOrderId, String shipmentOrderId) {
      // 1. addCart
      // 2. addCartConfirm → extraer shipmentsId
      // 3. saveGenerateParentOrder → extraer payId + payment info
      // 4. Verificar balance suficiente
      // 5. payBalanceV2
      // 6. Actualizar estado local
  }
  ```
  - Si algún paso falla: registrar en `cj_orders.fulfillment_step` + `fulfillment_error`
  - Si balance insuficiente: estado `AWAITING_FUNDS`, publicar Kafka `cj.order.awaiting_funds`

- [ ] **F2.4** Agregar columnas a tabla `cj_orders`:
  ```sql
  -- db.changelog-X.sql
  ALTER TABLE cj_orders ADD COLUMN fulfillment_step VARCHAR(30) DEFAULT 'CREATED';
  ALTER TABLE cj_orders ADD COLUMN fulfillment_error TEXT;
  ALTER TABLE cj_orders ADD COLUMN pay_id VARCHAR(50);
  ALTER TABLE cj_orders ADD COLUMN cj_actual_payment NUMERIC(18,2);
  ALTER TABLE cj_orders ADD COLUMN cj_postage_amount NUMERIC(18,2);
  ALTER TABLE cj_orders ADD COLUMN cj_product_amount NUMERIC(18,2);
  ALTER TABLE cj_orders ADD COLUMN shipments_id VARCHAR(100);
  -- fulfillment_step: CREATED, IN_CART, CONFIRMED, PARENT_GENERATED, PAID, FAILED, AWAITING_FUNDS
  ```

- [ ] **F2.5** Modificar `CjOrderFulfillmentUseCaseImpl.submitOrderToCj()`:
  - Después de `createOrderV3` exitoso, llamar automáticamente a `CjFulfillmentPipelineService.processFulfillment()`
  - Si falla el pipeline: programar retry vía `CjOrderRetryScheduler`

- [ ] **F2.6** Modificar `CjOrderRetryScheduler.java`:
  - Además de reintentar `createOrderV3`, también reintentar pipeline steps fallidos
  - Consultar `fulfillment_step` para saber desde qué paso reintentar (idempotente)
  - Si AWAITING_FUNDS: verificar balance antes de reintentar

- [ ] **F2.7** Crear endpoint admin `POST /api/v1/admin/cj/orders/{orderId}/retry-fulfillment`:
  - Permite reintentar manualmente el pipeline completo o desde un paso específico
  - Requiere rol ADMIN

- [ ] **F2.8** Publicar eventos Kafka del pipeline:
  - `cj.order.cart_added` — orden añadida al carrito CJ
  - `cj.order.paid` — pago CJ completado, CJ va a procesar
  - `cj.order.awaiting_funds` — balance insuficiente, requiere recarga
  - `cj.order.fulfillment_failed` — paso del pipeline falló (para alertas admin)

- [ ] **F2.9** Monitoreo de balance:
  - Scheduler cada hora: `getBalance()` → si < umbral configurable (`app.cj.min-balance-alert`, default $50 USD), publicar Kafka `cj.balance.low`
  - `mic-notificationservice` consume y envía alerta email al admin

- [ ] **F2.10** Tests unitarios:
  - `CjFulfillmentPipelineServiceTest` — happy path completo
  - Test de cada step individual con mocks
  - Test de retry desde step intermedio
  - Test de balance insuficiente → AWAITING_FUNDS

---

## FASE 3: Webhooks CJ (Notificaciones en Tiempo Real)

> **Microservicio:** `mic-orderservice` (receivers) + `mic-productcategory` (product/stock)
> **Endpoint CJ:** `POST /webhook/set`
> **Propósito:** Recibir eventos en tiempo real de CJ en lugar de polling cada 15 min.
>
> **Requisitos CJ:** HTTPS público, TLS 1.2+, responder 200 en <3 segundos

### Tipos de Webhook CJ

| Tipo | Payload | Acción |
|------|---------|--------|
| `ORDER` | cjOrderId, orderStatus, trackNumber, logisticName | Actualizar estado orden + tracking |
| `LOGISTIC` | orderId, trackingNumber, trackingStatus (0-14), logisticsTrackEvents | Actualizar eventos tracking |
| `PRODUCT` | pid, productStatus, fields[] | Actualizar producto (INSERT/UPDATE/DELETE) |
| `VARIANT` | vid, variantSellPrice, variantStatus, fields[] | Actualizar variante |
| `STOCK` | vid → areaId, countryCode, storageNum | Actualizar inventario |
| `ORDERSPLIT` | originalOrderId, splitOrderList[] | Manejar split de órdenes |

### Checklist Fase 3

- [ ] **F3.1** Crear `CjWebhookController.java` en `mic-orderservice`:
  ```java
  @RestController
  @RequestMapping("/api/v1/cj/webhook")
  public class CjWebhookController {
      @PostMapping("/order")     // → ORDER + ORDERSPLIT
      @PostMapping("/logistics") // → LOGISTIC
  }
  ```
  - Validar header/signature si CJ lo provee
  - Responder 200 OK inmediatamente
  - Procesar async vía Kafka interno

- [ ] **F3.2** Crear `CjWebhookController.java` en `mic-productcategory`:
  ```java
  @RestController
  @RequestMapping("/api/v1/cj/webhook")
  public class CjWebhookController {
      @PostMapping("/product") // → PRODUCT + VARIANT
      @PostMapping("/stock")   // → STOCK
  }
  ```

- [ ] **F3.3** Crear DTOs para webhooks:
  - `CjWebhookPayload`: messageId, type, messageType, params
  - `CjOrderWebhookParams`: cjOrderId, orderNumber, orderStatus, logisticName, trackNumber, trackingUrl, createDate, updateDate, payDate, deliveryDate, completeDate, orderItems[]
  - `CjLogisticWebhookParams`: orderId, logisticName, trackingNumber, trackingStatus (0-14), trackingUrl, logisticsTrackEvents (JSON string con eventos)
  - `CjProductWebhookParams`: pid, productStatus, productSellPrice, fields[]
  - `CjVariantWebhookParams`: vid, variantSellPrice, variantStatus, fields[]
  - `CjStockWebhookParams`: Map<vid, List<StockEntry>> (vid → areaId+countryCode+storageNum)
  - `CjOrderSplitWebhookParams`: originalOrderId, splitOrderList[], orderSplitTime

- [ ] **F3.4** Implementar handler ORDER webhook:
  - Buscar orden local por `cjOrderId` o `orderNumber`
  - Mapear orderStatus CJ → estado interno (misma lógica que `CjOrderStatusSyncScheduler`)
  - Si SHIPPED: extraer trackNumber + logisticName → actualizar orden
  - Si DELIVERED: marcar como entregado
  - Si CANCELLED: marcar como cancelado, publicar Kafka `order.cancelled`
  - Publicar Kafka `order.status.updated` si hubo cambio

- [ ] **F3.5** Implementar handler LOGISTIC webhook:
  - Crear tabla `cj_tracking_events`:
    ```sql
    CREATE TABLE cj_tracking_events (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      order_id UUID NOT NULL REFERENCES orders(id),
      cj_order_id VARCHAR(50),
      tracking_number VARCHAR(100),
      tracking_status INT NOT NULL,       -- 0-14 (CJ status codes)
      status_description VARCHAR(100),
      event_activity TEXT,
      event_location VARCHAR(200),
      event_time TIMESTAMP,
      logistic_name VARCHAR(100),
      tracking_url VARCHAR(500),
      raw_payload JSONB,
      created_at TIMESTAMP DEFAULT NOW()
    );
    CREATE INDEX idx_tracking_events_order ON cj_tracking_events(order_id);
    ```
  - Parsear `logisticsTrackEvents` JSON → insertar eventos individuales
  - Tracking statuses CJ: 0=Sin info, 1=Warehouse shipped, 2=Forwarder received, 3=Return, 4=Dispatched, 5=International transit, 6=Arrived destination, 7=Customs start, 8=Customs done, 9=Last-mile pickup, 10=Out for delivery, 11=Ready for pickup, 12=Delivered, 13=Failed/Exception, 14=Return
  - Si status 12 (Delivered): actualizar orden a DELIVERED
  - Si status 13/14 (Failed/Return): alertar admin vía Kafka

- [ ] **F3.6** Implementar handler PRODUCT/VARIANT webhook (en `mic-productcategory`):
  - PRODUCT INSERT: disparar fetch completo del producto vía `getProductDetail(pid)`
  - PRODUCT UPDATE: actualizar solo los campos en `fields[]`
  - PRODUCT DELETE (status=2 off sale): marcar producto como inactivo
  - VARIANT UPDATE: actualizar precio, dimensiones, status según `fields[]`

- [ ] **F3.7** Implementar handler STOCK webhook (en `mic-productcategory`):
  - Por cada vid en el payload: actualizar inventario del variant en la BD local
  - Si inventario llega a 0: marcar variante como out-of-stock

- [ ] **F3.8** Implementar handler ORDERSPLIT webhook:
  - Crear tabla `cj_order_splits`:
    ```sql
    CREATE TABLE cj_order_splits (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      original_cj_order_id VARCHAR(50) NOT NULL,
      split_cj_order_id VARCHAR(50) NOT NULL,
      order_id UUID REFERENCES orders(id),
      order_status INT,
      product_list JSONB,
      split_time TIMESTAMP,
      created_at TIMESTAMP DEFAULT NOW()
    );
    ```
  - Registrar la relación original → splits para tracking

- [ ] **F3.9** Registrar webhooks en CJ al iniciar la aplicación:
  - Crear `CjWebhookRegistrationService.java`:
    ```java
    @PostConstruct / @EventListener(ApplicationReadyEvent.class)
    public void registerWebhooks() {
        // POST /webhook/set con URLs de nuestros endpoints
        // product: ENABLE → https://{domain}/api/v1/cj/webhook/product
        // stock: ENABLE → https://{domain}/api/v1/cj/webhook/stock
        // order: ENABLE → https://{domain}/api/v1/cj/webhook/order
        // logistics: ENABLE → https://{domain}/api/v1/cj/webhook/logistics
    }
    ```
  - Configuración: `app.cj.webhook.base-url` (URL pública HTTPS)
  - Si no hay URL configurada: no registrar (modo desarrollo)

- [ ] **F3.10** Mantener el scheduler `CjOrderStatusSyncScheduler` como fallback:
  - Reducir frecuencia de 15 min a 1 hora (los webhooks son el mecanismo principal)
  - Solo procesa órdenes que NO han recibido webhook update en las últimas 2 horas
  - Logging de discrepancias webhook vs polling

- [ ] **F3.11** Idempotencia de webhooks:
  - Guardar `messageId` en tabla `cj_webhook_log`:
    ```sql
    CREATE TABLE cj_webhook_log (
      message_id VARCHAR(50) PRIMARY KEY,
      type VARCHAR(20) NOT NULL,
      message_type VARCHAR(20),
      processed_at TIMESTAMP DEFAULT NOW()
    );
    ```
  - Verificar `messageId` antes de procesar → ignorar duplicados

- [ ] **F3.12** Tests:
  - Test de cada handler con payloads reales de la documentación CJ
  - Test de idempotencia (mismo messageId 2 veces)
  - Test de ORDERSPLIT

---

## FASE 4: Tracking Detallado para el Cliente

> **Microservicio:** `mic-orderservice`
> **Endpoint CJ:** `GET /logistic/trackInfo`
> **Propósito:** Permitir al cliente consultar el estado detallado de su envío.

### Endpoint CJ: trackInfo

```
GET https://developers.cjdropshipping.com/api2.0/v1/logistic/trackInfo
    ?trackNumber=CJPKL7160102171YQ
Header: CJ-Access-Token: xxx

Response:
{
  "data": [
    {
      "trackingNumber": "CJPKL7160102171YQ",
      "logisticName": "CJPacket Sensitive",
      "trackingFrom": "CN",
      "trackingTo": "US",
      "deliveryDay": "13",
      "deliveryTime": "2021-06-17 07:04:04",
      "trackingStatus": "In transit",
      "lastMileCarrier": "CJPacket",
      "lastTrackNumber": "926112903032124"
    }
  ]
}
```

### Checklist Fase 4

- [ ] **F4.1** Agregar método `getTrackingInfo(List<String> trackNumbers)` en `CjShoppingClient.java`:
  - URL: `/logistic/trackInfo`
  - Método: GET con query params
  - Soporta batch (múltiples trackNumber params)
  - Resilience4j: retry 2x, timeout 15s

- [ ] **F4.2** Crear `TrackingPort.java` y `CjTrackingAdapter.java`:
  - `getTrackingInfo(String trackNumber)` → `TrackingInfoDto`
  - Incluir: trackingNumber, logisticName, trackingFrom, trackingTo, deliveryDay, deliveryTime, trackingStatus, lastMileCarrier, lastTrackNumber

- [ ] **F4.3** Crear `TrackingUseCaseImpl.java`:
  - Buscar orden del usuario por orderId
  - Verificar que la orden pertenece al usuario autenticado
  - Si hay trackNumber: consultar CJ trackInfo + combinar con eventos locales (tabla `cj_tracking_events` de webhooks)
  - Si no hay trackNumber aún: devolver solo el status actual de la orden
  - Cachear resultado trackInfo por 30 minutos

- [ ] **F4.4** Agregar columnas a tabla `orders`:
  ```sql
  ALTER TABLE orders ADD COLUMN tracking_url VARCHAR(500);
  ALTER TABLE orders ADD COLUMN last_mile_carrier VARCHAR(100);
  ALTER TABLE orders ADD COLUMN last_mile_track_number VARCHAR(100);
  ALTER TABLE orders ADD COLUMN estimated_delivery_days VARCHAR(20);
  ALTER TABLE orders ADD COLUMN delivery_time TIMESTAMP;
  ```

- [ ] **F4.5** Crear endpoint REST `GET /api/v1/orders/{orderId}/tracking`:
  - Response:
    ```json
    {
      "trackingNumber": "CJPKL7160102171YQ",
      "logisticName": "CJPacket Sensitive",
      "trackingStatus": "In transit",
      "estimatedDays": "5-10",
      "trackingUrl": "https://...",
      "lastMileCarrier": "USPS",
      "lastMileTrackNumber": "926112903032124",
      "events": [
        {
          "status": "Customs Clearance Completed",
          "location": "NEW YORK, US",
          "dateTime": "2024-01-15 14:30:00"
        }
      ]
    }
    ```
  - Autorización: solo el dueño de la orden

- [ ] **F4.6** Integrar con notificaciones:
  - Cuando webhook LOGISTIC llega con status 4 (Dispatched): email "Tu pedido ha sido enviado"
  - Cuando status 10 (Out for delivery): email "Tu pedido está en camino"
  - Cuando status 12 (Delivered): email "Tu pedido ha sido entregado"
  - Templates en `mic-notificationservice`
  - Kafka topics: `notification.shipping.dispatched`, `notification.shipping.out_for_delivery`, `notification.shipping.delivered`

- [ ] **F4.7** Tests unitarios para TrackingUseCase + adapter

---

## FASE 5: Cancelación de Órdenes CJ

> **Microservicio:** `mic-orderservice`
> **Endpoint CJ:** `DELETE /shopping/order/deleteOrder`
> **Propósito:** Permitir cancelar órdenes que aún no han sido enviadas.

### Checklist Fase 5

- [ ] **F5.1** Agregar método `deleteOrder(String orderId)` en `CjShoppingClient.java`:
  - URL: `/shopping/order/deleteOrder?orderId={orderId}`
  - Método: DELETE
  - Solo funciona si orden CJ está en status CREATED o IN_CART (antes de pago)

- [ ] **F5.2** Crear `CancelCjOrderUseCaseImpl.java`:
  - Verificar estado local: solo cancelar si no está SHIPPED/DELIVERED
  - Verificar estado CJ: solo cancelar si CREATED, IN_CART, o UNPAID
  - Si UNSHIPPED (ya pagado, no enviado): NO se puede cancelar vía API → crear dispute manual
  - Llamar `deleteOrder(cjOrderId)` en CJ
  - Actualizar estado local a CANCELLED
  - Publicar Kafka `order.cancelled`
  - Si había pago del cliente: iniciar refund flow

- [ ] **F5.3** Crear endpoint REST `POST /api/v1/orders/{orderId}/cancel`:
  - Body: `{ "reason": "..." }`
  - Autorización: dueño de la orden o ADMIN
  - Response: resultado de la cancelación

- [ ] **F5.4** Tests unitarios con todos los escenarios de estado

---

## FASE 6: Configuración y API Gateway

> **Microservicio:** `mic-apigatewayservice`
> **Propósito:** Configurar rutas para los nuevos endpoints y seguridad de webhooks.

### Checklist Fase 6

- [ ] **F6.1** Agregar rutas en API Gateway para nuevos endpoints:
  - `GET /api/v1/orders/freight/calculate` → mic-orderservice
  - `GET /api/v1/orders/{id}/tracking` → mic-orderservice
  - `POST /api/v1/orders/{id}/cancel` → mic-orderservice
  - `POST /api/v1/admin/cj/orders/{id}/retry-fulfillment` → mic-orderservice

- [ ] **F6.2** Configurar rutas de webhook CJ (sin autenticación JWT, CJ las llama directamente):
  - `POST /api/v1/cj/webhook/order` → mic-orderservice (PÚBLICO, sin JWT)
  - `POST /api/v1/cj/webhook/logistics` → mic-orderservice (PÚBLICO, sin JWT)
  - `POST /api/v1/cj/webhook/product` → mic-productcategory (PÚBLICO, sin JWT)
  - `POST /api/v1/cj/webhook/stock` → mic-productcategory (PÚBLICO, sin JWT)
  - Seguridad: IP whitelist CJ o validación por token secreto en query param

- [ ] **F6.3** Configurar rate limiting en webhook endpoints (máx 100 req/min por IP)

---

## Propiedades de Configuración Nuevas

```yaml
# mic-orderservice application.yaml (agregar a lo existente)
app:
  cj:
    # Freight
    freight-cache-ttl: ${CJ_FREIGHT_CACHE_TTL:30m}

    # Fulfillment pipeline
    auto-pay-enabled: ${CJ_AUTO_PAY_ENABLED:true}
    min-balance-alert: ${CJ_MIN_BALANCE_ALERT:50.00}
    balance-check-cron: ${CJ_BALANCE_CHECK_CRON:0 0 * * * *}

    # Webhooks
    webhook:
      enabled: ${CJ_WEBHOOK_ENABLED:false}
      base-url: ${CJ_WEBHOOK_BASE_URL:}  # https://tudominio.com
      secret: ${CJ_WEBHOOK_SECRET:}       # Para validación

    # Tracking
    tracking-cache-ttl: ${CJ_TRACKING_CACHE_TTL:30m}

    # Sync (modificar existente)
    status-sync-cron: ${CJ_STATUS_SYNC_CRON:0 0 * * * *}  # Cada hora (antes 15 min)
    fallback-sync-threshold: ${CJ_FALLBACK_SYNC_HOURS:2}   # Sync si no hay webhook en 2h
```

---

## Nuevos Kafka Topics

| Topic | Productor | Consumidor | Propósito |
|-------|-----------|------------|-----------|
| `cj.order.cart_added` | mic-orderservice | (log) | Orden añadida al carrito CJ |
| `cj.order.paid` | mic-orderservice | mic-notificationservice | Pago CJ completado |
| `cj.order.awaiting_funds` | mic-orderservice | mic-notificationservice | Balance insuficiente → alerta admin |
| `cj.order.fulfillment_failed` | mic-orderservice | mic-notificationservice | Pipeline falló → alerta admin |
| `cj.balance.low` | mic-orderservice | mic-notificationservice | Balance bajo umbral |
| `order.cancelled` | mic-orderservice | mic-notificationservice | Orden cancelada |
| `notification.shipping.dispatched` | mic-orderservice | mic-notificationservice | Email: pedido enviado |
| `notification.shipping.out_for_delivery` | mic-orderservice | mic-notificationservice | Email: en camino |
| `notification.shipping.delivered` | mic-orderservice | mic-notificationservice | Email: entregado |

---

## Migraciones de Base de Datos

### mic-orderservice (siguiente changelog después del último existente)
```sql
-- Fase 2: Fulfillment pipeline columns
ALTER TABLE cj_orders ADD COLUMN fulfillment_step VARCHAR(30) DEFAULT 'CREATED';
ALTER TABLE cj_orders ADD COLUMN fulfillment_error TEXT;
ALTER TABLE cj_orders ADD COLUMN pay_id VARCHAR(50);
ALTER TABLE cj_orders ADD COLUMN cj_actual_payment NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN cj_postage_amount NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN cj_product_amount NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN shipments_id VARCHAR(100);

-- Fase 3: Tracking events
CREATE TABLE cj_tracking_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id),
  cj_order_id VARCHAR(50),
  tracking_number VARCHAR(100),
  tracking_status INT NOT NULL,
  status_description VARCHAR(100),
  event_activity TEXT,
  event_location VARCHAR(200),
  event_time TIMESTAMP,
  logistic_name VARCHAR(100),
  tracking_url VARCHAR(500),
  raw_payload JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_tracking_events_order ON cj_tracking_events(order_id);
CREATE INDEX idx_tracking_events_tracking_number ON cj_tracking_events(tracking_number);

-- Fase 3: Order splits
CREATE TABLE cj_order_splits (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  original_cj_order_id VARCHAR(50) NOT NULL,
  split_cj_order_id VARCHAR(50) NOT NULL,
  order_id UUID REFERENCES orders(id),
  order_status INT,
  product_list JSONB,
  split_time TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_order_splits_original ON cj_order_splits(original_cj_order_id);

-- Fase 3: Webhook idempotency log
CREATE TABLE cj_webhook_log (
  message_id VARCHAR(50) PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  message_type VARCHAR(20),
  processed_at TIMESTAMP DEFAULT NOW()
);

-- Fase 4: Tracking columns on orders
ALTER TABLE orders ADD COLUMN tracking_url VARCHAR(500);
ALTER TABLE orders ADD COLUMN last_mile_carrier VARCHAR(100);
ALTER TABLE orders ADD COLUMN last_mile_track_number VARCHAR(100);
ALTER TABLE orders ADD COLUMN estimated_delivery_days VARCHAR(20);
ALTER TABLE orders ADD COLUMN delivery_time TIMESTAMP;
```

---

## Resumen de Archivos a Crear/Modificar

### Archivos NUEVOS (~25 archivos)

| Fase | Archivo | Microservicio |
|------|---------|---------------|
| F1 | `FreightCalculationPort.java` | mic-orderservice |
| F1 | `FreightOptionDto.java`, `FreightProductItem.java` | mic-orderservice |
| F1 | `CjFreightCalculationAdapter.java` | mic-orderservice |
| F1 | `FreightCalculationUseCaseImpl.java` | mic-orderservice |
| F1 | `FreightController.java` (o en OrderController) | mic-orderservice |
| F2 | `AddCartResult.java`, `AddCartConfirmResult.java` | mic-orderservice |
| F2 | `GenerateParentOrderResult.java` | mic-orderservice |
| F2 | `CjFulfillmentPipelineService.java` | mic-orderservice |
| F2 | `CjBalanceMonitorScheduler.java` | mic-orderservice |
| F3 | `CjWebhookController.java` (orderservice) | mic-orderservice |
| F3 | `CjWebhookController.java` (productcategory) | mic-productcategory |
| F3 | `CjWebhookPayload.java` + handlers DTOs (6+) | mic-orderservice |
| F3 | `CjOrderWebhookHandler.java` | mic-orderservice |
| F3 | `CjLogisticWebhookHandler.java` | mic-orderservice |
| F3 | `CjProductWebhookHandler.java` | mic-productcategory |
| F3 | `CjStockWebhookHandler.java` | mic-productcategory |
| F3 | `CjWebhookRegistrationService.java` | mic-orderservice |
| F3 | `CjTrackingEventEntity.java` | mic-orderservice |
| F3 | `CjOrderSplitEntity.java` | mic-orderservice |
| F3 | `CjWebhookLogEntity.java` | mic-orderservice |
| F4 | `TrackingPort.java`, `CjTrackingAdapter.java` | mic-orderservice |
| F4 | `TrackingUseCaseImpl.java` | mic-orderservice |
| F4 | `TrackingController.java` (o en OrderController) | mic-orderservice |
| F5 | `CancelCjOrderUseCaseImpl.java` | mic-orderservice |
| F6 | Rutas gateway_route SQL insert | mic-apigatewayservice |

### Archivos a MODIFICAR (~8 archivos)

| Archivo | Cambio |
|---------|--------|
| `CjShoppingClient.java` | +5 métodos (addCart, addCartConfirm, saveParent, payBalanceV2, deleteOrder, trackInfo, freightCalculate) |
| `CjOrderFulfillmentUseCaseImpl.java` | Invocar pipeline después de createOrderV3 |
| `CjOrderRetryScheduler.java` | Reintentar pipeline steps + balance check |
| `CjOrderStatusSyncScheduler.java` | Reducir frecuencia a 1h, filtrar por webhook freshness |
| `application.yaml` (orderservice) | +propiedades webhook, freight, balance |
| `application.yaml` (productcategory) | +propiedades webhook |
| DB changelog (orderservice) | +4 ALTER TABLE, +3 CREATE TABLE |
| DB changelog (productcategory) | (si hay cambios de webhook stock) |

---

## Orden de Implementación Recomendado

```
Fase 2 (Fulfillment Pipeline)  ←← PRIMERO — Sin esto CJ no envía nada
  ↓
Fase 1 (Freight Calculation)   ←← Mejora UX checkout
  ↓
Fase 3 (Webhooks)              ←← Reemplaza polling, tiempo real
  ↓
Fase 4 (Tracking detallado)    ←← Visibilidad para cliente
  ↓
Fase 5 (Cancelación)           ←← Gestión de cancelaciones
  ↓
Fase 6 (Gateway + Seguridad)   ←← Se va haciendo incremental con cada fase
```

> **Nota:** La Fase 6 (Gateway) se implementa incrementalmente con cada fase anterior,
> no es un bloque separado al final.

---

## Checklist General

| ID | Fase | Item | Estado |
|----|------|------|--------|
| F1.1 | 1 | FreightCalculationPort | [ ] |
| F1.2 | 1 | DTOs freight | [ ] |
| F1.3 | 1 | CjFreightCalculationAdapter | [ ] |
| F1.4 | 1 | CjShoppingClient.calculateFreight() | [ ] |
| F1.5 | 1 | FreightCalculationUseCaseImpl | [ ] |
| F1.6 | 1 | Endpoint REST freight | [ ] |
| F1.7 | 1 | Integrar logisticName en checkout | [ ] |
| F1.8 | 1 | Tests freight | [ ] |
| F2.1 | 2 | CjShoppingClient +4 métodos pipeline | [ ] |
| F2.2 | 2 | DTOs respuesta pipeline | [ ] |
| F2.3 | 2 | CjFulfillmentPipelineService | [ ] |
| F2.4 | 2 | ALTER TABLE cj_orders (pipeline cols) | [ ] |
| F2.5 | 2 | Integrar pipeline en submitOrderToCj | [ ] |
| F2.6 | 2 | Retry scheduler pipeline-aware | [ ] |
| F2.7 | 2 | Endpoint admin retry-fulfillment | [ ] |
| F2.8 | 2 | Kafka events pipeline | [ ] |
| F2.9 | 2 | Balance monitor scheduler | [ ] |
| F2.10 | 2 | Tests pipeline | [ ] |
| F3.1 | 3 | CjWebhookController (orderservice) | [ ] |
| F3.2 | 3 | CjWebhookController (productcategory) | [ ] |
| F3.3 | 3 | DTOs webhook | [ ] |
| F3.4 | 3 | Handler ORDER webhook | [ ] |
| F3.5 | 3 | Handler LOGISTIC webhook + tabla | [ ] |
| F3.6 | 3 | Handler PRODUCT/VARIANT webhook | [ ] |
| F3.7 | 3 | Handler STOCK webhook | [ ] |
| F3.8 | 3 | Handler ORDERSPLIT + tabla | [ ] |
| F3.9 | 3 | Registro webhooks en CJ | [ ] |
| F3.10 | 3 | Ajustar scheduler como fallback | [ ] |
| F3.11 | 3 | Idempotencia webhook_log | [ ] |
| F3.12 | 3 | Tests webhooks | [ ] |
| F4.1 | 4 | CjShoppingClient.getTrackingInfo() | [ ] |
| F4.2 | 4 | TrackingPort + adapter | [ ] |
| F4.3 | 4 | TrackingUseCaseImpl | [ ] |
| F4.4 | 4 | ALTER TABLE orders (tracking cols) | [ ] |
| F4.5 | 4 | Endpoint REST tracking | [ ] |
| F4.6 | 4 | Notificaciones shipping | [ ] |
| F4.7 | 4 | Tests tracking | [ ] |
| F5.1 | 5 | CjShoppingClient.deleteOrder() | [ ] |
| F5.2 | 5 | CancelCjOrderUseCaseImpl | [ ] |
| F5.3 | 5 | Endpoint REST cancel | [ ] |
| F5.4 | 5 | Tests cancelación | [ ] |
| F6.1 | 6 | Rutas gateway nuevos endpoints | [ ] |
| F6.2 | 6 | Rutas gateway webhooks (público) | [ ] |
| F6.3 | 6 | Rate limiting webhooks | [ ] |

**Total: 44 items | ~25 archivos nuevos | ~8 archivos modificados**
