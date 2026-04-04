package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false, length = 64)
    private String orderId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
