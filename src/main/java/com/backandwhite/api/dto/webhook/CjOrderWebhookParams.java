package com.backandwhite.api.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Params block for ORDER and ORDERSPLIT webhook events from CJ.
 * Field names match the CJ webhook documentation.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjOrderWebhookParams {

    /** Internal CJ order ID. */
    private String orderId;

    /** Shipment order ID (maps to our {@code cj_orders.shipment_order_id}). */
    private String shipmentOrderId;

    /** Current CJ order status, e.g. "CREATED", "IN_PRODUCTION", "SHIPPED". */
    private String orderStatus;

    /** Carrier tracking number (available once shipped). */
    private String trackNumber;

    /** Logistics provider name, e.g. "PostNL". */
    private String logisticName;

    /** Tracking URL provided by CJ. */
    private String trackUrl;
}
