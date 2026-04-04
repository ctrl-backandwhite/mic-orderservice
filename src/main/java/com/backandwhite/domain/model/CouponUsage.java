package com.backandwhite.domain.model;

import lombok.*;

import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {
    private String id;
    private String couponId;
    private String userId;
    private String orderId;
    private Instant usedAt;
}
