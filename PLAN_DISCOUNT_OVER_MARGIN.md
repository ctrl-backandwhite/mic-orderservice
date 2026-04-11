# Plan: Descuentos aplicados solo sobre el margen de ganancia

## Problema

Actualmente los descuentos de campañas (FLASH, PERCENTAGE, FIXED) se aplican sobre el **precio de venta completo** (retail = costo + margen). Esto permite que un descuento del 30% sobre un producto con 40% de margen venda **por debajo del precio de costo**, generando pérdidas.

### Ejemplo del problema actual

| Paso | Precio | Fórmula |
|------|--------|---------|
| Costo CJ | $6.11 | — |
| Margen 40% | $8.55 | 6.11 × 1.40 |
| Descuento 30% (actual) | **$5.99** | 8.55 × (1 − 0.30) |
| **Resultado** | **Pérdida de $0.12** | 5.99 < 6.11 |

### Comportamiento deseado

El descuento debe aplicarse **solo sobre el margen de ganancia**, nunca sobre el costo del proveedor.

| Paso | Precio | Fórmula |
|------|--------|---------|
| Costo CJ | $6.11 | — |
| Margen 40% → $2.44 | $8.55 | 6.11 × 1.40 |
| Descuento 30% **solo sobre margen** | **$7.82** | 6.11 + 2.44 × (1 − 0.30) |
| **Resultado** | **Ganancia de $1.71** | 7.82 > 6.11 ✅ |

## Fórmula nueva

```
margen = retailPrice − costPrice
descuentoPorcentaje: precioFinal = costPrice + margen × (1 − campaignValue / 100)
descuentoFijo:       precioFinal = max(costPrice, retailPrice − campaignValue)
```

**El precio nunca puede bajar del costo.**

## Puntos de cambio

### 1. Frontend — `useFlashDeals.ts` (display en storefront)

**Archivo:** `front/Ecomerce/src/app/hooks/useFlashDeals.ts`

**Antes:**
```ts
discountedPrice = originalPrice * (1 - campaignValue / 100);
```

**Después:**
```ts
const cost = mapped.costPrice ?? 0;
const margin = Math.max(0, originalPrice - cost);
if (camp.type === "FIXED") {
    discountedPrice = Math.max(cost, originalPrice - (camp.value ?? 0));
} else {
    discountedPrice = cost + margin * (1 - (camp.value ?? 0) / 100);
}
```

- `costPrice` ya está disponible en el tipo `Product` del frontend.
- El mapper `NexaProductMapper.ts` ya lo mapea desde `costPriceRaw`.

### 2. Backend — `CmsClient.java` en mic-orderservice (validación server-side)

**Archivo:** `back/mic-orderservice/src/.../infrastructure/client/CmsClient.java`

**Cambio:** El método `calculateBestCampaignDiscount()` necesita recibir `costPrice` además de `basePrice` para calcular el descuento solo sobre el margen.

**Antes:**
```java
case "PERCENTAGE", "FLASH" -> basePrice.percentage(value);
```

**Después:**
```java
case "PERCENTAGE", "FLASH" -> {
    Money margin = basePrice.subtract(costPrice);
    yield margin.percentage(value);  // descuento solo sobre el margen
}
```

### 3. Backend — `CatalogClient.java` en mic-orderservice (obtener costPrice)

**Archivo:** `back/mic-orderservice/src/.../infrastructure/client/CatalogClient.java`

**Problema adicional encontrado:** `CatalogClient` lee `variantSellPrice` del API response, que es el precio de costo CJ (no el retail). Debe leer `retailPrice` para el precio de venta y `variantSellPrice` para el costo.

**Cambio:** `ProductVerification` record debe incluir `costPrice`:
```java
record ProductVerification(BigDecimal price, BigDecimal costPrice, String categoryId, BigDecimal weight)
```

El client leerá:
- `retailPrice` del variant → `price` (precio de venta con margen)
- `variantSellPrice` del variant → `costPrice` (costo CJ)

### 4. Backend — `OrderUseCaseImpl.java` (pasar costPrice al cálculo)

**Archivo:** `back/mic-orderservice/src/.../application/usecase/impl/OrderUseCaseImpl.java`

Pasar `costPrice` al método `calculateBestCampaignDiscount`.

### 5. Backend — Interfaces `CatalogPort` y `CmsPort`

Actualizar el record `ProductVerification` y la firma de `calculateBestCampaignDiscount` para incluir `costPrice`.

## Archivos a modificar

| # | Servicio | Archivo | Cambio |
|---|----------|---------|--------|
| 1 | mic-orderservice | `CatalogPort.java` | Agregar `costPrice` a `ProductVerification` |
| 2 | mic-orderservice | `CatalogClient.java` | Leer `retailPrice` + `variantSellPrice` del variant |
| 3 | mic-orderservice | `CmsPort.java` | Agregar param `costPrice` a `calculateBestCampaignDiscount` |
| 4 | mic-orderservice | `CmsClient.java` | Aplicar descuento solo sobre margen |
| 5 | mic-orderservice | `OrderUseCaseImpl.java` | Pasar `costPrice` al cálculo de descuento |
| 6 | Ecomerce (React) | `useFlashDeals.ts` | Usar `costPrice` para calcular descuento sobre margen |

## Garantía de seguridad

- **PERCENTAGE/FLASH:** El descuento máximo posible = 100% del margen. Precio mínimo = costPrice.
- **FIXED:** `max(costPrice, retailPrice − fixedAmount)`. Nunca por debajo del costo.
- **Cupones:** Fuera del alcance de este cambio (sistema separado, se puede abordar después).

## Verificación

1. Producto con costo $6.11, margen 40% (retail $8.55):
   - Campaña FLASH 30%: $6.11 + $2.44×0.70 = **$7.82** ✅
   - Campaña FLASH 100%: $6.11 + $2.44×0.00 = **$6.11** (al costo, sin pérdida) ✅
   - Campaña FIXED $5.00: max($6.11, $8.55−$5.00) = **$6.11** ✅
2. Verificar que order service valide correctamente precios del carrito.
3. Verificar que el badge de descuento muestre el % correcto sobre el margen.
