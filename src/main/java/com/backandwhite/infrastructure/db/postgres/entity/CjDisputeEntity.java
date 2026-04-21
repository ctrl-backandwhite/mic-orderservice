package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Customer-initiated return / dispute against a CJ order (Fase 13). Mirrors
 * CJ's dispute lifecycle; a single row evolves from OPEN → UNDER_REVIEW →
 * APPROVED/REJECTED → CLOSED. If APPROVED, {@code refundAmount} tells the
 * payment service how much to refund through the original gateway.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_disputes")
public class CjDisputeEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "cj_order_id", length = 200)
    private String cjOrderId;

    @Column(name = "cj_dispute_id", length = 200)
    private String cjDisputeId;

    @Column(name = "reason", length = 80)
    private String reason;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_urls", columnDefinition = "jsonb")
    private String evidenceUrls;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "resolution", length = 30)
    private String resolution;

    @Column(name = "refund_amount", precision = 18, scale = 4)
    private BigDecimal refundAmount;

    @Column(name = "refund_currency", length = 3)
    private String refundCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
