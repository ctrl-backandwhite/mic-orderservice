package com.backandwhite.application.port.out;

public interface OrderEventPort {

    // ── Order Events ─────────────────────────────────────────────────────────

    void publishOrderCreated(String orderId, String userId, String email,
            String orderReference, String totalAmount,
            String status, int itemCount, String shippingAddressId);

    void publishOrderConfirmed(String orderId, String userId, String email,
            String orderReference, String totalAmount, int itemCount);

    void publishOrderStatusUpdated(String orderId, String userId, String email,
            String orderReference, String previousStatus, String newStatus);

    void publishOrderCancelled(String orderId, String userId, String email,
            String orderReference, String reason);

    void publishOrderShipped(String orderId, String userId, String email,
            String orderReference, String trackingNumber,
            String carrier, String estimatedDelivery);

    void publishOrderDelivered(String orderId, String userId, String email,
            String orderReference, String totalAmount);

    void publishOrderReturnRequested(String orderId, String returnRequestId,
            String userId, String email,
            String orderReference, String reason);

    void publishOrderReturnApproved(String orderId, String returnRequestId,
            String userId, String email,
            String orderReference, String refundAmount);

    // ── Cart Events ──────────────────────────────────────────────────────────

    void publishCartAbandoned(String cartId, String userId, String email,
            String totalAmount, int itemCount, String lastActivityAt);

    void publishCartCheckoutInitiated(String cartId, String orderId, String userId,
            String email, String totalAmount, int itemCount,
            String couponCode, String shippingAddressId);

    // ── Stock Events ─────────────────────────────────────────────────────────

    void publishStockReservation(String productId, String variantId,
            String orderId, int quantity);

    void publishStockDeducted(String productId, String variantId,
            String orderId, int quantity);
}
