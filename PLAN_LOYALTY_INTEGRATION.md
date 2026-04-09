# Plan de Integración del Sistema de Loyalty

## Resumen Ejecutivo

Integrar el programa de fidelización al flujo de órdenes para que:
1. **Cada dólar gastado = 1 punto** de loyalty (multiplicado por el tier multiplier)
2. Los niveles se asignan dinámicamente según los tiers configurados en el admin
3. Los puntos se otorgan al confirmar la orden (no al entregar, para mejor UX)
4. Se detecta y notifica el cambio de nivel cuando el usuario sube de tier

---

## Estado Actual del Sistema

### Lo que YA existe

| Componente | Ubicación | Estado |
|---|---|---|
| **Tiers CRUD** (admin) | `mic-cmsservice` → `LoyaltyController` | ✅ Funcional |
| **Rules CRUD** (admin) | `mic-cmsservice` → `LoyaltyController` | ✅ Funcional |
| **Transaction ledger** | `mic-cmsservice` → `loyalty_transactions` table | ✅ Funcional |
| **Balance query** | `GET /api/v1/loyalty/balance` | ✅ Funcional |
| **Earn points API** | `POST /api/v1/loyalty/earn` | ✅ Funcional |
| **Redeem points API** | `POST /api/v1/loyalty/redeem` | ✅ Funcional |
| **Kafka consumer** | `CmsOrderEventConsumerService` → listen `order.delivered` | ✅ Existe pero **nunca recibe eventos** |
| **Admin UI** | `AdminLoyalty.tsx` | ✅ Tiers + Rules CRUD |
| **Checkout redemption** | `LoyaltySection.tsx` + `useLoyaltyRedemption.ts` | ✅ Frontend integrado |
| **Profile display** | `ProfileDatos.tsx` | ⚠️ Tiers **hardcodeados** |

### Configuración actual en BD (mic-cmsservice, puerto 5406)

**Tiers:**
| Nombre | Min Points | Max Points | Multiplicador |
|---|---|---|---|
| Bronze | 0 | 999 | 1.0x |
| Silver | 1,000 | 2,999 | 1.5x |
| Gold | 3,000 | 4,999 | 2.0x |
| Platinum | 5,000 | 99,999 | 3.0x |

**Rules:**
| Acción | Puntos por unidad | Activa |
|---|---|---|
| PURCHASE | 1 punto por $1 | ✅ |
| REVIEW | 50 puntos | ✅ |
| REFERRAL | 200 puntos | ✅ |
| REGISTRATION | 100 puntos | ❌ |

### Lo que NO existe (gaps a cubrir)

| Gap | Descripción | Impacto |
|---|---|---|
| **`order.delivered` nunca se publica** | `updateStatus()` usa `publishOrderStatusUpdated()` genérico, nunca llama `publishOrderDelivered()` | CMS consumer nunca recibe el evento → nunca otorga puntos |
| **No hay LoyaltyMachine** | No existe lógica centralizada para calcular puntos, resolver tier, detectar level-up | Cálculo disperso y sin detección de cambio de nivel |
| **Frontend tiers hardcodeados** | `ProfileDatos.tsx` tiene 4 tiers fijos en código | Admin puede cambiar tiers pero el perfil no refleja cambios |
| **No hay bonus por nivel** | Al subir de tier, no se otorgan puntos bonus | Falta gamificación |

---

## Arquitectura de la Solución

### Flujo completo propuesto

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE (mic-orderservice)                  │
│                                                                     │
│  1. updateStatus(DELIVERED)                                         │
│     └─→ publishOrderDelivered() ──→ Kafka [order.delivered]         │
│                                                                     │
└────────────────────────────────────┬────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    CMS SERVICE (mic-cmsservice)                      │
│                                                                     │
│  2. CmsOrderEventConsumerService                                    │
│     └─→ consume [order.delivered]                                   │
│         └─→ LoyaltyMachine.processOrderDelivery(userId, total, orderId) │
│                                                                     │
│  3. LoyaltyMachine (NUEVO)                                          │
│     ├─ a) Obtener PURCHASE rule → pointsPerUnit (default: 1)       │
│     ├─ b) Obtener balance actual del usuario                        │
│     ├─ c) Resolver tier actual → multiplier                         │
│     ├─ d) Calcular: points = floor(totalUSD × pointsPerUnit × mult)│
│     ├─ e) Registrar transacción EARN                                │
│     ├─ f) Calcular nuevo balance                                    │
│     ├─ g) Resolver nuevo tier                                       │
│     ├─ h) Si tier cambió → otorgar bonus + publicar evento level-up │
│     └─ i) Publicar evento points-earned                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Implementación Detallada

### PASO 1: Fix — Publicar `order.delivered` en mic-orderservice

**Archivo**: `OrderUseCaseImpl.java` (línea ~445)

**Cambio**: En `updateStatus()`, cuando `newStatus == DELIVERED`, publicar el evento específico de delivery además del genérico.

```java
// Después de publishOrderStatusUpdated():
if (newStatus == OrderStatus.DELIVERED) {
    orderEventPort.publishOrderDelivered(
        updated.getId(),
        updated.getUserId(),
        null, // email (resolved downstream)
        updated.getOrderNumber(),
        updated.getTotal().toPlainString()
    );
}
```

**Impacto**: El topic `order.delivered` ahora recibirá eventos con `userId`, `orderId`, y `totalAmount`.

---

### PASO 2: Crear `LoyaltyMachine` en mic-cmsservice

**Archivo nuevo**: `application/service/LoyaltyMachine.java`

**Responsabilidades**:
1. **Calcular puntos**: `floor(totalUSD × pointsPerUnit × tierMultiplier)`
2. **Resolver tier**: Dado un balance de puntos, encontrar el tier correspondiente
3. **Detectar level-up**: Comparar tier antes vs después de otorgar puntos
4. **Otorgar bonus**: Si hay level-up, agregar transacción EARN con description "Level up bonus: {tierName}"
5. **Publicar eventos**: Points earned + level-up notification

```java
@Service
@RequiredArgsConstructor
public class LoyaltyMachine {
    private final LoyaltyRepository loyaltyRepository;
    private final CmsEventPort cmsEventPort;

    /**
     * Proceso principal: calcular y otorgar puntos por compra,
     * detectar cambio de nivel, otorgar bonus.
     */
    @Transactional
    public LoyaltyResult processOrderDelivery(String userId, BigDecimal totalAmountUsd, String orderId) {
        // 1. Obtener regla PURCHASE activa
        int pointsPerUnit = loyaltyRepository.findActiveRuleByAction(LoyaltyAction.PURCHASE)
                .map(LoyaltyRule::getPointsPerUnit).orElse(1);

        // 2. Balance y tier ANTES
        int balanceBefore = loyaltyRepository.sumPointsByUserId(userId);
        LoyaltyTier tierBefore = resolveTier(balanceBefore);
        BigDecimal multiplier = tierBefore != null && tierBefore.getMultiplier() != null
                ? tierBefore.getMultiplier() : BigDecimal.ONE;

        // 3. Calcular puntos de la compra
        int earnedPoints = totalAmountUsd
                .multiply(BigDecimal.valueOf(pointsPerUnit))
                .multiply(multiplier)
                .intValue();

        if (earnedPoints <= 0) return LoyaltyResult.empty();

        // 4. Registrar transacción EARN (compra)
        loyaltyRepository.saveTransaction(LoyaltyTransaction.builder()
                .userId(userId)
                .points(earnedPoints)
                .type(LoyaltyTransactionType.EARN)
                .description("Purchase reward: " + earnedPoints + " pts (x" + multiplier + ")")
                .orderId(orderId)
                .createdAt(Instant.now())
                .build());

        // 5. Calcular nuevo balance y tier
        int balanceAfter = balanceBefore + earnedPoints;
        LoyaltyTier tierAfter = resolveTier(balanceAfter);

        // 6. Detectar level-up
        boolean leveledUp = tierAfter != null && (tierBefore == null
                || !tierAfter.getId().equals(tierBefore.getId()));
        int bonusPoints = 0;

        if (leveledUp) {
            // Bonus: puntsPerUnit × 100 como bono de nivel (configurable)
            bonusPoints = pointsPerUnit * 100;
            loyaltyRepository.saveTransaction(LoyaltyTransaction.builder()
                    .userId(userId)
                    .points(bonusPoints)
                    .type(LoyaltyTransactionType.EARN)
                    .description("🎉 Level up bonus! Welcome to " + tierAfter.getName())
                    .orderId(orderId)
                    .createdAt(Instant.now())
                    .build());
            balanceAfter += bonusPoints;
        }

        // 7. Publicar evento
        String tierName = tierAfter != null ? tierAfter.getName() : "Bronze";
        cmsEventPort.publishLoyaltyPointsEarned(
                userId, orderId, earnedPoints + bonusPoints, balanceAfter, tierName);

        return LoyaltyResult.builder()
                .earnedPoints(earnedPoints)
                .bonusPoints(bonusPoints)
                .totalBalance(balanceAfter)
                .previousTier(tierBefore != null ? tierBefore.getName() : null)
                .currentTier(tierName)
                .leveledUp(leveledUp)
                .build();
    }

    /**
     * Resolver tier dado un balance de puntos.
     * Itera tiers ordenados por minPoints ASC, último que matchea gana.
     */
    public LoyaltyTier resolveTier(int points) {
        List<LoyaltyTier> tiers = loyaltyRepository.findAllTiers(); // sorted by minPoints ASC
        LoyaltyTier resolved = null;
        for (LoyaltyTier tier : tiers) {
            if (points >= tier.getMinPoints()) {
                resolved = tier;
            } else {
                break;
            }
        }
        return resolved;
    }
}
```

**Nuevo DTO de resultado**:
```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class LoyaltyResult {
    private int earnedPoints;
    private int bonusPoints;
    private int totalBalance;
    private String previousTier;
    private String currentTier;
    private boolean leveledUp;
    public static LoyaltyResult empty() { return new LoyaltyResult(); }
}
```

---

### PASO 3: Actualizar `CmsOrderEventConsumerService`

**Cambio**: Reemplazar la lógica inline por la llamada a `LoyaltyMachine`.

```java
@KafkaListener(topics = AppConstants.KAFKA_TOPIC_ORDER_DELIVERED, ...)
public void onOrderDelivered(OrderDeliveredEvent event) {
    String userId = str(event.getUserId());
    String orderId = str(event.getOrderId());
    BigDecimal amount = parseAmount(str(event.getTotalAmount()));
    if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

    LoyaltyResult result = loyaltyMachine.processOrderDelivery(userId, amount, orderId);
    log.info("::> Loyalty processed: userId={}, earned={}, bonus={}, tier={}, levelUp={}",
            userId, result.getEarnedPoints(), result.getBonusPoints(),
            result.getCurrentTier(), result.isLeveledUp());
}
```

---

### PASO 4: Fix frontend — Tiers dinámicos en `ProfileDatos.tsx`

**Cambio**: Reemplazar la constante `TIERS` hardcodeada por llamada a la API.

```tsx
// ANTES:
const TIERS = [
  { name: "Bronce", min: 0, max: 999, ... },
  ...
];

// DESPUÉS:
const [tiers, setTiers] = useState<LoyaltyTier[]>([]);
useEffect(() => {
    loyaltyRepository.findAllTiers().then(setTiers).catch(() => {});
}, []);

function getMembershipTier(points: number) {
    const sorted = [...tiers].sort((a, b) => a.minPoints - b.minPoints);
    let match = sorted[0];
    for (const t of sorted) {
        if (points >= t.minPoints) match = t;
        else break;
    }
    return match;
}
```

---

## Fórmula de Puntos

```
earnedPoints = floor(totalUSD × pointsPerUnit × tierMultiplier)
```

| Variable | Fuente | Valor actual |
|---|---|---|
| `totalUSD` | `OrderDeliveredEvent.totalAmount` | Monto total de la orden en USD |
| `pointsPerUnit` | `loyalty_rules` WHERE action='PURCHASE' | 1 (configurable en admin) |
| `tierMultiplier` | `loyalty_tiers` según balance del usuario | 1.0x–3.0x según tier |

### Ejemplos (con config actual)

| Total orden | Tier actual | Multiplicador | Puntos ganados |
|---|---|---|---|
| $50.00 | Bronze | 1.0x | 50 pts |
| $50.00 | Silver | 1.5x | 75 pts |
| $50.00 | Gold | 2.0x | 100 pts |
| $50.00 | Platinum | 3.0x | 150 pts |

### Level-up bonus

Cuando un usuario sube de tier, recibe **100 puntos bonus** (= `pointsPerUnit × 100`). Esto es configurable cambiando el `pointsPerUnit` de la regla PURCHASE en el admin.

---

## Archivos a Modificar

### mic-orderservice (backend de órdenes)
| Archivo | Cambio |
|---|---|
| `OrderUseCaseImpl.java` | Añadir `publishOrderDelivered()` cuando status → DELIVERED |

### mic-cmsservice (backend CMS)
| Archivo | Cambio |
|---|---|
| **`LoyaltyMachine.java`** (NUEVO) | Lógica centralizada de cálculo de puntos, tier resolution, level-up |
| **`LoyaltyResult.java`** (NUEVO) | DTO de resultado del proceso |
| `CmsOrderEventConsumerService.java` | Reemplazar lógica inline con `LoyaltyMachine` |
| `LoyaltyUseCaseImpl.java` | Ya existente, `earnPoints()` sigue siendo el punto de entrada para el ledger |

### Ecomerce (frontend React)
| Archivo | Cambio |
|---|---|
| `ProfileDatos.tsx` | Reemplazar TIERS hardcodeados por API call a `/api/v1/loyalty/tiers` |

---

## Flujo de Eventos Kafka

```
┌──────────────┐    shipping.order.delivered    ┌──────────────────┐
│  Shipping    │ ──────────────────────────────→ │  Order Service   │
│  Service     │                                 │  (consumer)      │
└──────────────┘                                 └────────┬─────────┘
                                                          │
                                                 updateStatus(DELIVERED)
                                                          │
                                                 publishOrderDelivered()
                                                          │
                                                          ▼
                                                  order.delivered (Kafka)
                                                          │
                                                          ▼
                                                 ┌────────────────────┐
                                                 │  CMS Service       │
                                                 │  (consumer)        │
                                                 │  LoyaltyMachine    │
                                                 │  → earn points     │
                                                 │  → detect level-up │
                                                 │  → bonus points    │
                                                 └────────┬───────────┘
                                                          │
                                                 loyalty.points.earned (Kafka)
                                                          │
                                                          ▼
                                                 ┌────────────────────┐
                                                 │  Notification Svc  │
                                                 │  (email/push)      │
                                                 └────────────────────┘
```

---

## Orden de Ejecución

1. ✅ Crear `LoyaltyMachine.java` en mic-cmsservice
2. ✅ Crear `LoyaltyResult.java` en mic-cmsservice
3. ✅ Actualizar `CmsOrderEventConsumerService.java` para usar LoyaltyMachine
4. ✅ Fix `OrderUseCaseImpl.java` para publicar `order.delivered`
5. ✅ Fix `ProfileDatos.tsx` para tiers dinámicos
6. ✅ Rebuild mic-cmsservice + mic-orderservice
7. ✅ Test end-to-end

---

## Riesgos y Mitigaciones

| Riesgo | Mitigación |
|---|---|
| Kafka deshabilitado en local | NoOp adapters ya existen, verificar `spring.kafka.enabled` |
| Doble conteo si evento se procesa 2 veces | Verificar idempotencia: check si ya existe tx con mismo orderId antes de earn |
| Total de la orden en moneda local (no USD) | El `totalAmount` en el evento debe ser en USD, o usar `exchangeRateToUsd` para convertir |
| Tiers vacíos en BD | `resolveTier()` retorna null → multiplier defaults a 1.0 |
