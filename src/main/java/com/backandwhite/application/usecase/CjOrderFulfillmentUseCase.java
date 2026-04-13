package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.CjOrder;

public interface CjOrderFulfillmentUseCase {

    /**
     * Submits a confirmed order to CJ Dropshipping and automatically
     * triggers the fulfillment pipeline (addCart → confirm → pay).
     */
    CjOrder submitOrderToCj(String orderId);

    /**
     * Syncs the CJ order status and tracking number.
     * Called by the status-sync scheduler.
     */
    CjOrder syncStatus(String orderId);

    /**
     * Returns the CJ order record for the given internal order ID.
     */
    CjOrder findByOrderId(String orderId);

    /**
     * Cancels a CJ order via the CJ API (only allowed before payment).
     */
    void cancelCjOrder(String orderId);
}
