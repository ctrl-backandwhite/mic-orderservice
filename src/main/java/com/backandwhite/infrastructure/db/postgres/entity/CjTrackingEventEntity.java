package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One tracking milestone event for a CJ shipment, received via the LOGISTIC
 * webhook
 * or polled from the CJ tracking API.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_tracking_events")
public class CjTrackingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    /** Internal order UUID. */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /** CJ order ID. */
    @Column(name = "cj_order_id", length = 100)
    private String cjOrderId;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /** High-level status, e.g. IN_TRANSIT, DELIVERED. */
    @Column(name = "tracking_status", length = 50)
    private String trackingStatus;

    @Column(name = "status_description", length = 255)
    private String statusDescription;

    /** Milestone activity description. */
    @Column(name = "event_activity", columnDefinition = "text")
    private String eventActivity;

    @Column(name = "event_location", length = 255)
    private String eventLocation;

    /** Parsed event timestamp. */
    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "logistic_name", length = 100)
    private String logisticName;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    /** Full raw JSON from CJ for debugging / reprocessing. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Object rawPayload;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
