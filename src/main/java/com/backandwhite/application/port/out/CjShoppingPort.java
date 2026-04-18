package com.backandwhite.application.port.out;

import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.Order;
import java.util.List;

public interface CjShoppingPort {

    /**
     * Submits a confirmed order to CJ Dropshipping and returns the CJ order record.
     */
    CjOrder createOrder(Order order);

    /**
     * Fetches the current status and tracking number from CJ for a given CJ order
     * ID.
     */
    CjOrder getOrderDetail(String cjOrderId);

    /** Queries CJ account balance. */
    String getBalance();

    /**
     * Queries CJ account balance as a numeric value (USD). Returns null if
     * unavailable.
     */
    java.math.BigDecimal getBalanceAmount();

    // ── Fulfillment Pipeline ─────────────────────────────────────────────────

    /** Step 1: Adds a CJ order to the shopping cart. */
    CjFulfillmentResult addCart(String cjOrderId);

    /** Step 2: Confirms the cart and obtains the shipmentsId. */
    CjFulfillmentResult addCartConfirm(String cjOrderId);

    /** Step 3: Generates parent order and obtains payId and payment breakdown. */
    CjFulfillmentResult generateParentOrder(String shipmentOrderId);

    /** Step 4: Pays the CJ balance for the generated parent order. */
    void payBalanceV2(String shipmentOrderId, String payId);

    // ── Logistics ────────────────────────────────────────────────────────────

    /**
     * Calculates freight options from CN to the given destination country for the
     * given variants.
     */
    List<CjFreightOption> calculateFreight(String toCountryCode, List<CjFreightOption.ProductItem> products);

    /** Fetches tracking details for the given tracking number. */
    CjTrackInfo getTrackInfo(String trackNumber);

    // ── Order Lifecycle ──────────────────────────────────────────────────────

    /** Deletes/cancels a CJ order (only allowed before payment). */
    void deleteOrder(String cjOrderId);
}
