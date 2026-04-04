package com.backandwhite.domain.model;

import lombok.*;

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
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal rate;
    private BigDecimal freeAbove;
    private int estimatedDays;
    private boolean active;
    private String carrierName;
    private Instant createdAt;
    private Instant updatedAt;
}
