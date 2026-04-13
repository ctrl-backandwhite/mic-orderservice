package com.backandwhite.api.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Params block for ORDERSPLIT webhook events.
 * CJ may split one order into multiple child orders when products are
 * sourced from different warehouses.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjOrderSplitWebhookParams {

    /** The original parent CJ order ID that was split. */
    private String orderId;

    /** Human-readable order reference number. */
    private String orderNum;

    /** List of sub-orders generated from the original. */
    private List<SubOrder> subOrders;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubOrder {
        /** New CJ order ID for this sub-order. */
        private String cjOrderId;
        /** Products included in this sub-order (raw JSON field from CJ). */
        private String productList;
        /** Status of this sub-order. */
        private String orderStatus;
    }
}
