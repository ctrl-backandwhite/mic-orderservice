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
    private int estimatedDays;
    @Builder.Default
    private boolean active = true;
    private String carrierName;
    private Instant createdAt;
    private Instant updatedAt;
}
