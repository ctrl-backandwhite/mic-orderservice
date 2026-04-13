package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_orders")
public class CjOrderEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "cj_order_id", length = 200)
    private String cjOrderId;

    @Column(name = "shipment_order_id", length = 200)
    private String shipmentOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cj_order_status", nullable = false, length = 50)
    @Builder.Default
    private CjOrderStatus cjOrderStatus = CjOrderStatus.CREATED;

    @Column(name = "track_number", length = 200)
    private String trackNumber;

    @Column(name = "logistic_name", length = 200)
    private String logisticName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_info_list", columnDefinition = "jsonb")
    private Map<String, Object> productInfoList;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private int errorCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    // ── Fulfillment pipeline columns ──────────────────────────────────────────

    @Column(name = "fulfillment_step", length = 30)
    private String fulfillmentStep;

    @Column(name = "fulfillment_error", columnDefinition = "TEXT")
    private String fulfillmentError;

    @Column(name = "pay_id", length = 50)
    private String payId;

    @Column(name = "shipments_id", length = 100)
    private String shipmentsId;

    @Column(name = "cj_actual_payment", precision = 18, scale = 2)
    private BigDecimal cjActualPayment;

    @Column(name = "cj_postage_amount", precision = 18, scale = 2)
    private BigDecimal cjPostageAmount;

    @Column(name = "cj_product_amount", precision = 18, scale = 2)
    private BigDecimal cjProductAmount;

    @Column(name = "last_webhook_at")
    private Instant lastWebhookAt;
}
