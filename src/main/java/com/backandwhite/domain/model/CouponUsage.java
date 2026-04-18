package com.backandwhite.domain.model;

import java.time.Instant;
import lombok.*;

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
