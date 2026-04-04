package com.backandwhite.domain.model;

import com.backandwhite.domain.valureobject.CouponType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private String id;
    private String code;
    private CouponType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private Integer maxUses;
    private int usedCount;
    private Integer maxUsesPerUser;
    private Instant validFrom;
    private Instant validUntil;
    private List<String> appliesToCategories;
    private List<String> appliesToProducts;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
