package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRule {
    private String id;
    private String carrierId;
    private String zone;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;
    private Money minPrice;
    private Money maxPrice;
    private Money rate;
    private Money freeAbove;
    /**
     * Maximum weight (kg) at which the {@link #freeAbove} threshold still grants
     * free shipping. Above this weight the rule charges the full {@link #rate}
     * regardless of the order subtotal — protects the margin on bulky shipments.
     * Null = no weight cap on the free-shipping promo.
     */
    private BigDecimal freeAboveMaxWeight;
    private int estimatedDays;
    @Builder.Default
    private boolean active = true;
    private String carrierName;
    private Instant createdAt;
    private Instant updatedAt;
}
