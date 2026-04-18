package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import lombok.experimental.SuperBuilder;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupon_usages")
public class CouponUsageEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "coupon_id", nullable = false, length = 64)
    private String couponId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;
}
