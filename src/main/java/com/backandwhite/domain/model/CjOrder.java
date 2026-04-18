package com.backandwhite.domain.model;

import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CjOrder {
    private String id;
    private String orderId;
    private String cjOrderId;
    private String shipmentOrderId;
    private CjOrderStatus cjOrderStatus;
    private String trackNumber;
    private String logisticName;
    private Map<String, Object> productInfoList;
    private Instant lastSyncedAt;
    private int errorCount;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    // ── Fulfillment pipeline fields ──────────────────────────────────────────

    /** Current step in the fulfillment pipeline. */
    private String fulfillmentStep;

    /** Error message from the last failed fulfillment step. */
    private String fulfillmentError;

    /** payId returned by saveGenerateParentOrder — needed for payBalanceV2. */
    private String payId;

    /** shipmentsId returned by addCartConfirm. */
    private String shipmentsId;

    /** Actual payment amount in USD as returned by CJ. */
    private BigDecimal cjActualPayment;

    /** Postage amount as returned by CJ. */
    private BigDecimal cjPostageAmount;

    /** Product amount as returned by CJ. */
    private BigDecimal cjProductAmount;

    /** Timestamp when the last webhook was received for this order. */
    private Instant lastWebhookAt;
}
