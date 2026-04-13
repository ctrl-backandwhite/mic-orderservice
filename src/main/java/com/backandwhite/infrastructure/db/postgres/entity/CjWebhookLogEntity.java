package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

/**
 * Stores processed CJ webhook message IDs for idempotent processing.
 * The {@code message_id} supplied by CJ is the primary key — attempting to
 * insert a duplicate will fail fast, preventing double-processing.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_webhook_log")
public class CjWebhookLogEntity {

    /** CJ-assigned message ID — used as the idempotency key. */
    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;

    /** Top-level event category (ORDER, LOGISTIC, ORDERSPLIT…). */
    @Column(name = "type", length = 30)
    private String type;

    /** Detailed event type (ORDER_CREATED, SHIPPING…). */
    @Column(name = "message_type", length = 50)
    private String messageType;

    /** Full raw JSON payload stored for debugging / replay. */
    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    /** When this webhook was persisted / processed. */
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
