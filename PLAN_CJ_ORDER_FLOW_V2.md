# PLAN: Flujo de Compra CJ Dropshipping — Correcciones, Multi-idioma y Multi-moneda

> Fecha: 2026-04-12 | Versión: 1.0
> Servicios: `mic-orderservice`, `mic-productcategory`, `mic-cmsservice`, front `Ecomerce`

---

## 0. Diagnóstico del Estado Actual

### ¿Está implementado el flujo de agregar al carrito llamando a CJ?

**Sí, está implementado en el back-end**, pero con **cuatro errores críticos** y varias mejoras pendientes.

El flujo back-end completo está en `CjFulfillmentPipelineService` y se dispara automáticamente cuando el cliente confirma el pedido (`POST /api/v1/orders`). El **front-end nunca llama a CJ directamente**; el back-end gestiona todo el pipeline de forma asíncrona.

### Pipeline implementado (actualizado a V3 + V2):
```
Cliente confirma pedido → [mic-orderservice]
  1. createOrderV3          POST /shopping/order/createOrderV3
  2. addCart                POST /shopping/order/addCart
  3. addCartConfirm         POST /shopping/order/addCartConfirm         → obtiene shipmentsId
  4. saveGenerateParentOrder POST /shopping/order/saveGenerateParentOrder → obtiene payId + paymentInfo
  5. payBalanceV2           POST /shopping/pay/payBalanceV2
```

### Errores críticos identificados:

| # | Archivo | Problema | Impacto |
|---|---------|----------|---------|
| E1 | `CjCreateOrderV3RequestDto.java` | Los campos de dirección están en un objeto nested `shippingAddress` pero la API espera campos **planos** (`shippingCountryCode`, `shippingProvince`, etc.) | **CRÍTICO: el pedido nunca se crea correctamente en CJ** |
| E2 | `CjShoppingClient.java` (getBalance) | Usa `/shopping/account/getAccountBalance` en lugar de `/shopping/pay/getBalance`. El DTO mapea `balance`/`currency` pero la API devuelve `amount`/`noWithdrawalAmount`/`freezeAmount` | **CRÍTICO: balance siempre null** → scheduler falla |
| E3 | `CjShoppingClient.java` (getOrderDetail) | Usa `/shopping/order/getOrderDetailByOrderId` → endpoint no documentado. El correcto es `/shopping/order/getOrderDetail?orderId=` | **MEDIO: sync de estado puede fallar** |
| E4 | `CjCreateOrderV3ResponseDto.java` | Faltan campos `shipmentOrderId`, `orderStatus`, `actualPayment`, `productAmount`, `postageAmount`, `interceptOrderReasons` | **MEDIO: datos de respuesta incompletos** |

### Gaps de producto (no errores, sino funcionalidad faltante):
- Front-end no muestra opciones de envío CJ en el checkout
- Front-end no muestra el tracking CJ en la página de seguimiento
- No hay capa de traducción para datos de productos CJ (solo en inglés)
- Los precios CJ (USD) no se convierten a la moneda del usuario en el checkout
- El `vid` (CJ variant ID) del carrito debe propagarse correctamente al crear la orden CJ

---

## 1. Correcciones Críticas — Back-end

### ✅ Tarea 1.1 — Refactorizar `CjCreateOrderV3RequestDto` a campos planos

**Archivo:** `mic-orderservice/src/main/java/com/backandwhite/infrastructure/client/cj/dto/CjCreateOrderV3RequestDto.java`

Reemplazar el objeto nested `ShippingAddressDto` por campos planos al nivel raíz,
siguiendo exactamente los nombres de la API:

```java
// ELIMINAR la clase nested ShippingAddressDto
// AÑADIR campos planos:

@JsonProperty("orderNumber")      private String orderNumber;
@JsonProperty("shippingCountryCode")  private String shippingCountryCode;   // "US"
@JsonProperty("shippingCountry")      private String shippingCountry;       // "United States"
@JsonProperty("shippingProvince")     private String shippingProvince;
@JsonProperty("shippingCity")         private String shippingCity;
@JsonProperty("shippingCounty")       private String shippingCounty;        // opcional
@JsonProperty("shippingZip")          private String shippingZip;
@JsonProperty("shippingPhone")        private String shippingPhone;
@JsonProperty("shippingCustomerName") private String shippingCustomerName;  // fullName
@JsonProperty("shippingAddress")      private String shippingAddress;       // calle
@JsonProperty("shippingAddress2")     private String shippingAddress2;
@JsonProperty("houseNumber")          private String houseNumber;
@JsonProperty("email")                private String email;
@JsonProperty("taxId")                private String taxId;
@JsonProperty("remark")               private String remark;
@JsonProperty("logisticName")         private String logisticName;
@JsonProperty("fromCountryCode")      private String fromCountryCode;       // "CN"
@JsonProperty("iossType")             private Integer iossType;             // default: null (no IOSS)
@JsonProperty("shopLogisticsType")    private Integer shopLogisticsType;    // default: 2 (seller logistics)
@JsonProperty("platform")            private String platform;              // "API"
@JsonProperty("products")            private List<CjOrderProductItemDto> products;
```

**Archivo:** `CjShoppingClient.java` → `buildCreateOrderRequest(Order order)`

Actualizar el método para mapear el modelo `Order` a los campos planos:

```java
String fullName = str(addr.get("fullName"));
return CjCreateOrderV3RequestDto.builder()
    .orderNumber(order.getOrderNumber())
    .shippingCountryCode(str(addr.get("country")))        // ISO 2-letter
    .shippingCountry(str(addr.get("countryName")))        // nombre completo
    .shippingProvince(str(addr.get("region")))
    .shippingCity(str(addr.get("city")))
    .shippingZip(str(addr.get("postalCode")))
    .shippingPhone(str(addr.get("phone")))
    .shippingCustomerName(fullName)                       // fullName, no split
    .shippingAddress(str(addr.get("street")))
    .shippingAddress2(str(addr.get("street2")))
    .houseNumber(str(addr.get("houseNumber")))
    .email(str(addr.get("email")))
    .logisticName(defaultLogisticName)
    .fromCountryCode(fromCountryCode)
    .shopLogisticsType(2)                                 // 2 = seller logistics
    .platform("API")
    .products(products)
    .remark(order.getNotes())
    .build();
```

También actualizar `CjOrderProductItemDto` para incluir `sku` y `storeLineItemId`:
```java
@JsonProperty("sku")            private String sku;
@JsonProperty("storeLineItemId") private String storeLineItemId;    // = order item ID
@JsonProperty("unitPrice")       private BigDecimal unitPrice;
```

---

### ✅ Tarea 1.2 — Corregir endpoint y DTO de Balance

**Archivo:** `CjShoppingClient.java` (ambos métodos `getBalance()` y `getBalanceAmount()`)

Cambiar la URI:
```java
// ANTES:
.uri("/shopping/account/getAccountBalance")
// DESPUÉS:
.uri("/shopping/pay/getBalance")
```

**Archivo:** `CjBalanceResponseDto.java`

La API devuelve `amount`, `noWithdrawalAmount`, `freezeAmount` (todos BigDecimal):
```java
@JsonProperty("amount")               private BigDecimal amount;
@JsonProperty("noWithdrawalAmount")   private BigDecimal noWithdrawalAmount;
@JsonProperty("freezeAmount")         private BigDecimal freezeAmount;
// ELIMINAR: balance, currency, creditBalance
```

Actualizar `getBalance()` y `getBalanceAmount()` para leer `data.getAmount()` en lugar de `data.getBalance()`.

---

### ✅ Tarea 1.3 — Corregir endpoint de `getOrderDetail`

**Archivo:** `CjShoppingClient.java` (método `getOrderDetail`)

```java
// ANTES:
.path("/shopping/order/getOrderDetailByOrderId")
// DESPUÉS:
.path("/shopping/order/getOrderDetail")
```

---

### ✅ Tarea 1.4 — Completar `CjCreateOrderV3ResponseDto`

Añadir campos que devuelve la API que son útiles para el pipeline:

```java
@JsonProperty("orderId")           private String orderId;             // CJ order ID
@JsonProperty("orderNum")          private String orderNum;
@JsonProperty("shipmentOrderId")   private String shipmentOrderId;     // IMPORTANTE: disponible desde createOrderV3
@JsonProperty("orderStatus")       private String orderStatus;         // "CREATED"
@JsonProperty("postageAmount")     private BigDecimal postageAmount;
@JsonProperty("productAmount")     private BigDecimal productAmount;
@JsonProperty("actualPayment")     private BigDecimal actualPayment;
@JsonProperty("logisticsMiss")     private Boolean logisticsMiss;      // alerta si logística no encontrada
@JsonProperty("interceptOrderReasons") private List<InterceptReason> interceptOrderReasons;
@JsonProperty("productInfoList")   private List<CjProductInfoListItemDto> productInfoList;

// Nested:
public static class InterceptReason {
    @JsonProperty("code")    private Integer code;
    @JsonProperty("message") private String message;
}
```

**Nota sobre `shipmentOrderId`:** La API V3 devuelve `shipmentOrderId` en la propia respuesta de `createOrderV3`. Sin embargo, el pipeline actual obtiene el `shipmentsId` de la respuesta de `addCartConfirm`. Ambos pueden coexistir: usar el de `addCartConfirm` si viene, fallback al de `createOrderV3`.

---

### ✅ Tarea 1.5 — Completar `CjGenerateParentOrderResponseDto.PaymentInformation`

Los campos del docs son:
```java
@JsonProperty("actualPayment")       private BigDecimal actualPayment;
@JsonProperty("balanceDeduction")    private BigDecimal balanceDeduction;
@JsonProperty("billAmount")          private BigDecimal billAmount;
@JsonProperty("canDeduct")           private Boolean canDeduct;
@JsonProperty("commodityDiscount")   private BigDecimal commodityDiscount;
@JsonProperty("commodityTotalAmount") private BigDecimal commodityTotalAmount;
@JsonProperty("couponAmount")        private BigDecimal couponAmount;
@JsonProperty("freight")             private BigDecimal freight;         // renombrar de postage
@JsonProperty("iossAmount")          private BigDecimal iossAmount;
@JsonProperty("iossTaxHandlingFee")  private BigDecimal iossTaxHandlingFee;
@JsonProperty("orderOriginalAmount") private BigDecimal orderOriginalAmount;
@JsonProperty("orderProductAmount")  private BigDecimal orderProductAmount;  // renombrar de productAmount
@JsonProperty("payableAmount")       private BigDecimal payableAmount;
@JsonProperty("serviceFee")          private BigDecimal serviceFee;
```

Actualizar `CjFulfillmentPipelineService` y `CjFulfillmentResult` para propagar `freight` (no `postage`) y `orderProductAmount`.

---

### ✅ Tarea 1.6 — Verificar que `vid` del carrito llega a la orden CJ

**Archivos:**
- `Order.java` (domain model)
- `OrderItem.java`
- `CjShoppingClient.buildCreateOrderRequest()` → mapeo `item.getVariantId()` → `vid`

Verificar que cuando el frontend agrega al carrito con `variantId` (que es el CJ `vid`), ese valor llega correctamente al `CjOrderProductItemDto.vid`.

**Si `variantId` es nulo**, usar `sku` como fallback. El `vid` en productos de CJ está en la tabla `product_variants.external_id` (o equivalente en mic-productcategory).

---

## 2. Multi-idioma — Traducciones de Datos CJ

### Contexto
La API de CJ devuelve toda la información (nombres de productos, variantes, categorías) **en inglés**. Necesitamos que el site muestre estos datos en los idiomas configurados (es, en, pt, fr como mínimo).

La traducción es **manual desde el admin** ya que CJ no ofrece API de traducción.

### ✅ Tarea 2.1 — Modelo de datos de traducciones (mic-productcategory)

**Archivo nuevo:** `src/main/resources/db/changelog/db.changelog-X.X.sql`

```sql
-- Tabla de traducciones para entidades del catálogo CJ
CREATE TABLE product_translations (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id   VARCHAR(100) NOT NULL,           -- ID del producto CJ
    locale       VARCHAR(10)  NOT NULL,            -- 'es', 'en', 'pt', 'fr'
    field_name   VARCHAR(50)  NOT NULL,            -- 'name', 'description', 'shortDescription'
    value        TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, locale, field_name)
);

CREATE INDEX idx_prod_trans_product ON product_translations(product_id);
CREATE INDEX idx_prod_trans_locale  ON product_translations(locale);

-- Traducción de variantes
CREATE TABLE variant_translations (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    variant_id   VARCHAR(100) NOT NULL,
    locale       VARCHAR(10)  NOT NULL,
    field_name   VARCHAR(50)  NOT NULL,            -- 'variantKey', 'variantValue'
    value        TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (variant_id, locale, field_name)
);

-- Traducción de categorías
CREATE TABLE category_translations (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    category_id  VARCHAR(100) NOT NULL,
    locale       VARCHAR(10)  NOT NULL,
    field_name   VARCHAR(50)  NOT NULL,            -- 'name', 'description'
    value        TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (category_id, locale, field_name)
);
```

### ✅ Tarea 2.2 — Entidades JPA + Repositorios (mic-productcategory)

**Archivos nuevos:**
- `domain/model/ProductTranslation.java`
- `domain/repository/ProductTranslationRepository.java`
- `infrastructure/db/postgres/entity/ProductTranslationEntity.java`
- `infrastructure/db/postgres/repository/ProductTranslationJpaRepository.java`
- `infrastructure/db/postgres/repository/impl/ProductTranslationRepositoryImpl.java`

### ✅ Tarea 2.3 — Use cases (mic-productcategory)

**Archivo nuevo:** `application/usecase/TranslationUseCase.java`

```java
public interface TranslationUseCase {
    void upsertProductTranslation(String productId, String locale, String field, String value);
    void upsertVariantTranslation(String variantId, String locale, String field, String value);
    void upsertCategoryTranslation(String categoryId, String locale, String field, String value);
    Map<String, String> getProductTranslations(String productId, String locale);
    Map<String, String> getVariantTranslations(String variantId, String locale);

    // Bulk upsert para el sync CJ
    void bulkUpsertProductTranslations(List<TranslationEntry> entries);
}
```

### ✅ Tarea 2.4 — API de traducciones (mic-productcategory)

**Archivo nuevo:** `api/controller/TranslationController.java`

```
PUT  /api/v1/admin/translations/products/{productId}     (admin: upsert traducción)
PUT  /api/v1/admin/translations/variants/{variantId}     (admin: upsert traducción)
PUT  /api/v1/admin/translations/categories/{categoryId}  (admin: upsert traducción)
GET  /api/v1/translations/products/{productId}?locale=es  (público: obtener traducción)
```

El `GET` de productos/variantes ya existente debe aceptar `?locale=es` y devolver los campos traducidos si existen, con fallback al inglés.

### ✅ Tarea 2.5 — Propagación al front-end

**Archivos a modificar en front:**
- `NexaProductRepository.ts` → añadir `locale` como param en los listados y detalle de productos
- `ProductDetail.tsx` → pasar `locale` desde `i18n` o settings del site
- `Cart.tsx` → mostrar nombre del producto en el idioma del usuario
- Admin `AdminProductsPage.tsx` → añadir pestaña/sección "Traducciones" por producto

**Configuración de locales soportados** (admin setting):
- Guardar en mic-cmsservice/settings: clave `supported_locales` = `["es","en","pt","fr"]`
- Añadir componente `TranslationEditor` en el admin para cada entidad con traducciones

---

## 3. Multi-moneda

### Contexto
- CJ cobra en **USD**. Los campos `actualPayment`, `postageAmount`, `productAmount` siempre son USD.
- El front-end ya tiene `CurrencyContext` con `convertFromUsd()` y `formatPrice()`.
- La `Order` ya tiene `currencyCode` y `exchangeRateToUsd`.
- **Gap:** Los precios de flete CJ en el checkout se muestran en USD sin conversión.

### ✅ Tarea 3.1 — Endpoint de flete con moneda (back-end)

**Archivo:** `ShippingController.java` → endpoint `GET /api/v1/shipping/freight/calculate`

Añadir parámetro `currency` y `exchangeRate`:
```
GET /api/v1/shipping/freight/calculate?toCountry=US&vid=xxx&qty=1&currency=EUR&rate=0.92
```

Respuesta:
```json
{
  "options": [
    {
      "logisticName": "CJPacket Ordinary",
      "priceUsd": 3.50,
      "priceLocal": 3.22,
      "currency": "EUR",
      "estimatedDays": "7-12 días"
    }
  ]
}
```

### ✅ Tarea 3.2 — Front-end: flete en moneda del usuario en el checkout

**Archivo:** `src/app/pages/checkout/hooks/useShippingOptions.ts`

Cuando se llama a `shippingRepository.getOptions()` (o al nuevo `freight/calculate`), convertir los precios CJ de USD a la moneda activa usando `convertFromUsd(priceUsd)`.

**Archivo:** `src/app/repositories/ShippingRepository.ts`

Añadir método `getCjFreightOptions({ toCountry, items, currency })` que llame al nuevo endpoint.

### ✅ Tarea 3.3 — Guardar tipo de cambio en la orden

Al crear la orden, guardar el `exchangeRateToUsd` activo en `Order.exchangeRateToUsd` (ya existe en el modelo). Esto permite calcular retroactivamente el monto CJ en la moneda local en el historial.

### ✅ Tarea 3.4 — Mostrar costos CJ en el historial del usuario

**Archivo:** `OrderTracking.tsx` y `UserProfile.tsx` (historial de órdenes)

Si la orden tiene `cjActualPayment` (USD), mostrar el equivalente en la moneda del usuario:
```
"Enviado con CJ Dropshipping · Costo: $3.50 USD (≈ 3.22 EUR)"
```

---

## 4. Mejoras en el Flujo de Compra — Front-end

### ✅ Tarea 4.1 — Opciones de Envío CJ en el Checkout

**Problema:** El checkout usa opciones de envío genéricas del `ShippingRepository`. Los productos CJ necesitan usar la API de flete de CJ.

**Lógica propuesta:**
- Detectar si los items del carrito son productos CJ (tienen `variantId` que es un CJ `vid`)
- Si son CJ: llamar a `/api/v1/shipping/freight/calculate?toCountry=XX&vid=VVV&qty=N`
- Si no son CJ: usar las opciones de envío genéricas

**Archivos a modificar:**
- `src/app/pages/checkout/hooks/useShippingOptions.ts` → lógica CJ vs genérico
- `src/app/repositories/ShippingRepository.ts` → añadir `getCjFreightOptions()`
- `src/app/context/CartContext.tsx` → añadir flag `hasCjItems` para notificar al checkout

**CartItemDto** (back-end):
```java
// Añadir campo a CartItemDto para que el front sepa si es producto CJ:
private boolean cjProduct;  // = variantId != null && isCjVid(variantId)
```

### ✅ Tarea 4.2 — Tracking CJ en `OrderTracking.tsx`

**Problema:** La página de seguimiento muestra eventos genéricos. Si la orden tiene un número de tracking CJ, debería mostrar los eventos en tiempo real desde CJ.

**Back-end:** El endpoint `GET /api/v1/tracking/orders/{orderId}/cj` ya fue implementado en el plan anterior y devuelve info de tracking de CJ.

**Front-end:**

**Archivo:** `src/app/repositories/TrackingRepository.ts`

```typescript
// Añadir:
async getCjTracking(orderId: string): Promise<CjTrackingInfo | null> {
    const res = await authFetch(`${BASE_URL}/orders/${orderId}/cj`);
    if (res.status === 404) return null;
    return handleRes<CjTrackingInfo>(res);
}

export interface CjTrackingInfo {
    trackingNumber: string;
    logisticName: string;
    trackingFrom: string;
    trackingTo: string;
    deliveryDay: string;
    deliveryTime: string;
    trackingStatus: string;
    lastMileCarrier: string | null;
    lastTrackNumber: string | null;
    // Eventos del webhook (tabla cj_tracking_events)
    events: CjTrackEvent[];
}

export interface CjTrackEvent {
    eventTime: string;
    description: string;
    location: string | null;
    carrier: string | null;
}
```

**Archivo:** `src/app/pages/OrderTracking.tsx`

- Si la orden tiene tracking CJ, llamar a `trackingRepository.getCjTracking(orderId)`
- Mostrar el carrier real, número de tracking del último tramo (`lastMileCarrier`/`lastTrackNumber`)
- Mostrar los eventos de `cj_tracking_events` ordenados por `eventTime` desc
- Enlace a `trackingUrl` (ya está en `Order.trackingUrl`)

### ✅ Tarea 4.3 — Estado del pedido CJ en el perfil del usuario

**Archivo:** `src/app/pages/UserProfile.tsx` (sección "Mis Pedidos")

Si la orden tiene `cjOrderStatus`:
- CREATED / IN_CART / UNPAID → "Procesando con proveedor"
- UNSHIPPED → "Pagado · En preparación"
- SHIPPED → "Enviado · Ver tracking"
- DELIVERED → "Entregado"
- CANCELLED → "Cancelado"

Mapear el estado CJ a un badge visual coordinado con el estado interno de la orden.

---

## 5. Mejoras en el Endpoint de Creación de Orden CJ

### ✅ Tarea 5.1 — Añadir `shopLogisticsType` configurable

Añadir en `application.yaml` de `mic-orderservice`:
```yaml
app:
  cj:
    shop-logistics-type: 2        # 2=seller logistics (default), 3=CJ platform logistics
    storage-id:                   # Solo si shopLogisticsType=1, dejar vacío por defecto
```

Leer estos valores en `CjShoppingClient` con `@Value` y propagarlos al request.

### ✅ Tarea 5.2 — Manejo de `logisticsMiss: true` en respuesta de createOrderV3

Si la respuesta incluye `logisticsMiss: true`, registrar una advertencia y notificar al administrador (Kafka event o log de alerta) para que seleccione manualmente una logística alternativa.

### ✅ Tarea 5.3 — Manejo de `interceptOrderReasons` en respuesta

Si la respuesta de `createOrderV3` incluye `interceptOrderReasons`, registrar los motivos en `CjOrder.fulfillmentError` y marcar el estado como `PIPELINE_FAILED` para que el retry scheduler lo reintente.

---

## 6. Cambios en `mic-productcategory` para soporte de traducciones

### ✅ Tarea 6.1 — Endpoint de producto con locale

**Archivo:** `api/controller/ProductController.java`

```java
// Añadir parámetro opcional:
@GetMapping("/{id}")
public ResponseEntity<ProductDtoOut> getById(
    @PathVariable String id,
    @RequestParam(defaultValue = "en") String locale
) { ... }
```

El use case debe:
1. Obtener el producto normalmente
2. Llamar a `translationRepository.find(productId, locale)`
3. Si existe traducción para `name` → sobreescribir `product.getName()` con el valor traducido
4. Si no existe → devolver nombre en inglés (fallback)

### ✅ Tarea 6.2 — Sincronización CJ: guardar nombre en inglés como traducción semilla

Cuando el sync CJ crea/actualiza un producto, también debe crear automáticamente la entrada en `product_translations` para `locale=en` con el nombre original. Esto permite que el admin vea cuál es la traducción base para cada idioma.

---

## 7. Cambios en el Gateway (mic-apigatewayservice)

### ✅ Tarea 7.1 — Rutas de traducciones

En `RouteSeeder.java`, añadir:
```java
// Admin translations (mic-productcategory)
.route("translations-admin", r -> r
    .path("/api/v1/admin/translations/**")
    .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(adminRateLimiter)))
    .uri(services.productcategory()))

// Public translations (mic-productcategory)
.route("translations-public", r -> r
    .path("/api/v1/translations/**")
    .uri(services.productcategory()))
```

---

## 8. Resumen de Archivos a Crear/Modificar

### mic-orderservice — Correcciones

| Acción | Archivo | Razón |
|--------|---------|-------|
| MODIFICAR | `dto/CjCreateOrderV3RequestDto.java` | Cambiar a campos planos (E1) |
| MODIFICAR | `dto/CjBalanceResponseDto.java` | Alinear con nueva API: `amount` (E2) |
| MODIFICAR | `dto/CjCreateOrderV3ResponseDto.java` | Añadir campos faltantes (E4) |
| MODIFICAR | `dto/CjGenerateParentOrderResponseDto.java` | Completar PaymentInformation (E5) |
| MODIFICAR | `CjShoppingClient.java` | Corregir 3 endpoints + buildCreateOrderRequest (E1,E2,E3) |
| MODIFICAR | `application.yaml` | Añadir `shop-logistics-type`, `storage-id` |
| MODIFICAR | `ShippingController.java` | Añadir parámetro `currency`/`rate` en freight |

### mic-orderservice — Front ABI

| Acción | Archivo | Razón |
|--------|---------|-------|
| MODIFICAR | `api/controller/TrackingController.java` | Asegurar que devuelve eventos de `cj_tracking_events` |
| MODIFICAR | `api/controller/CartController.java` | Añadir `cjProduct: boolean` a `CartItemDto` |

### mic-productcategory — Traducciones

| Acción | Archivo | Razón |
|--------|---------|-------|
| CREAR | `db/changelog/db.changelog-X.X.sql` | 3 tablas de traducciones |
| CREAR | `domain/model/ProductTranslation.java` | Modelo de dominio |
| CREAR | `domain/repository/ProductTranslationRepository.java` | Puerto |
| CREAR | `infrastructure/db/postgres/entity/ProductTranslationEntity.java` | JPA |
| CREAR | `infrastructure/db/postgres/repository/ProductTranslationJpaRepository.java` | JPA |
| CREAR | `application/usecase/TranslationUseCase.java` | Interfaz |
| CREAR | `application/usecase/impl/TranslationUseCaseImpl.java` | Implementación |
| CREAR | `api/controller/TranslationController.java` | API REST |
| MODIFICAR | `api/controller/ProductController.java` | Añadir `?locale=` |
| MODIFICAR | `application/service/CjSyncService.java` | Crear traducción `en` en el sync |

### Front-end (Ecomerce)

| Acción | Archivo | Razón |
|--------|---------|-------|
| MODIFICAR | `repositories/TrackingRepository.ts` | Añadir `getCjTracking()` |
| MODIFICAR | `pages/OrderTracking.tsx` | Mostrar tracking CJ real |
| MODIFICAR | `pages/UserProfile.tsx` | Mostrar estado CJ en historial |
| MODIFICAR | `pages/checkout/hooks/useShippingOptions.ts` | Opciones CJ si hay productos CJ |
| MODIFICAR | `repositories/ShippingRepository.ts` | Añadir `getCjFreightOptions()` |
| MODIFICAR | `repositories/NexaProductRepository.ts` | Pasar `locale` en listados |
| MODIFICAR | `pages/ProductDetail.tsx` | Pasar `locale` al cargar producto |
| CREAR | `pages/admin/components/TranslationEditor.tsx` | UI para gestionar traducciones |
| MODIFICAR | `pages/admin/AdminProductsPage.tsx` | Añadir tab de traducciones |

### mic-apigatewayservice

| Acción | Archivo | Razón |
|--------|---------|-------|
| MODIFICAR | `RouteSeeder.java` | Añadir rutas `/api/v1/admin/translations/**` y `/api/v1/translations/**` |

---

## 9. Orden de Implementación Recomendado

```
Fase 1 - Correcciones críticas (back-end, puede hacerse en ~1 día)
  → 1.1 Refactorizar CjCreateOrderV3RequestDto (campos planos)
  → 1.2 Corregir endpoint + DTO de balance
  → 1.3 Corregir endpoint getOrderDetail
  → 1.4 Completar CjCreateOrderV3ResponseDto
  → 1.5 Completar GenerateParentOrderResponseDto.PaymentInformation
  → Compilar y verificar BUILD OK

Fase 2 - Multi-moneda (front-end + back-end, ~1 día)
  → 3.1 Endpoint freight con parámetro currency/rate
  → 3.2 Front checkout: precios de flete en moneda activa
  → 3.3 Guardar exchangeRateToUsd en la orden
  → 3.4 Historial: mostrar costo CJ convertido

Fase 3 - Tracking CJ en front (front-end, ~0.5 día)
  → 4.2 CjTrackingInfo en TrackingRepository
  → 4.2 OrderTracking.tsx consume tracking CJ
  → 4.3 UserProfile: badges de estado CJ

Fase 4 - Multi-idioma (back + front, ~2-3 días)
  → 2.1 Migración DB (3 tablas de traducciones)
  → 2.2-2.4 Entidades, use cases, API de traducciones
  → 2.5 Front: locale en productos, TranslationEditor admin
  → 6.1 ProductController con ?locale=
  → 6.2 CJ sync: guardar traducción base en inglés

Fase 5 - Opciones de envío CJ en checkout (~1 día)
  → 4.1 Detección de items CJ en carrito
  → 4.1 useShippingOptions: usar flete CJ si hay items CJ
  → Gateway: nuevas rutas de traducciones
```

---

## 10. Notas Adicionales

### Por qué createOrderV3 en lugar de V2
V3 es la versión más reciente. V2 tenía el parámetro `payType` que permitía seleccionar el método de pago en la llamada; en V3 ya no existe porque el flujo siempre pasa por addCart → addCartConfirm → generateParentOrder → payBalanceV2. Usamos **siempre V3**.

### Por qué payBalanceV2 en lugar de payBalance
`payBalance` (v1) solo requiere `orderId`. `payBalanceV2` requiere `shipmentOrderId` + `payId` ambos obtenidos de `generateParentOrder`, lo que hace el flujo más preciso y auditable. Usamos **siempre payBalanceV2**.

### Sobre el flujo completo vs `payType=3`
El servidor nunca usará `payType` (ni `createOrderV2`). Si en el futuro se necesita crear una orden sin pagar (ej. pedidos pendientes de aprobación), se puede usar `payType=3` de V2, pero por ahora no se necesita.

### Idiomas soportados sugeridos
`es` (español), `en` (inglés - base CJ), `pt` (portugués), `fr` (francés).
Los idiomas activos se configuran en el setting del CMS (`supported_locales`).

### Monedas y tasa de cambio
Las tasas de cambio ya vienen de `CurrencyRateRepository.ts` del front con `CurrencyContext`. Al crear la orden se guarda el tipo de cambio vigente en `orders.exchange_rate_to_usd`. Los precios CJ **siempre se almacenan en USD** y se convierten solo para mostrar.
