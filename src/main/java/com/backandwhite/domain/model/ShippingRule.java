package com.backandwhite.domain.model;

import lombok.*;

import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import java.time.Instant;

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
    private boolean active;
    private String carrierName;
    private Instant createdAt;
    private Instant updatedAt;
}
