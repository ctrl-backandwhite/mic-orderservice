package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Event-sourced transitions for {@code cj_orders} — mirrors
 * {@link OrderStateHistoryEntity} but tracks CJ-side status (UNPAID, UNSHIPPED,
 * SHIPPED…) instead of local status (Fase 8.2).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_order_state_history")
public class CjOrderStateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cj_order_id", nullable = false, length = 200)
    private String cjOrderId;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    @Column(name = "actor", nullable = false, length = 20)
    private String actor;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}
