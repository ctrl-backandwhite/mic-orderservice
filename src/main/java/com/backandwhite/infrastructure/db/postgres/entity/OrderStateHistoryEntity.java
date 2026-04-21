package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Event-sourced transitions for {@code orders} (Fase 8.2). Replaces the
 * destructive {@code order_status} updates with an append-only log so we can
 * reconstruct the timeline of any order months later.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_state_history")
public class OrderStateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    /** SYSTEM | WEBHOOK | ADMIN | CUSTOMER | SCHEDULER */
    @Column(name = "actor", nullable = false, length = 20)
    private String actor;

    @Column(name = "actor_id", length = 120)
    private String actorId;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}
