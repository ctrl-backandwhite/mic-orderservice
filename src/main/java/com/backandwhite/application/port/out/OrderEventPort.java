package com.backandwhite.application.port.out;

public interface OrderEventPort {

        // ── Order Events ─────────────────────────────────────────────────────────

        void publishOrderCreated(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currencyCode,
                        String status, int itemCount, String shippingAddressId);

        void publishOrderConfirmed(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currencyCode, int itemCount);

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
                        String email, String totalAmount, String currencyCode, int itemCount,
                        String couponCode, String shippingAddressId);

        // ── Stock Events ─────────────────────────────────────────────────────────

        void publishStockReservation(String productId, String variantId,
                        String orderId, int quantity);

        void publishStockDeducted(String productId, String variantId,
                        String orderId, int quantity);
        // ── Notification Events ──────────────────────────────────────────────

        /**
         * Publishes an email notification event with the invoice data to the
         * notification.email.send topic so the notification service renders
         * and delivers the invoice email.
         */
        void publishInvoiceEmail(String email, String subject, String templateName,
                        java.util.Map<String, String> variables);

        // ── Saga Events (Compensation) ────────────────────────────────────────────

        /**
         * Publishes a saga.order.payment-requested event to trigger the payment step.
         */
        void publishSagaPaymentRequested(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currency);

        /**
         * Publishes a saga.order.stock-release event to undo stock reservation
         * during compensation after a payment failure.
         */
        void publishSagaStockRelease(String orderId, String userId, String reason);

        /**
         * Publishes a saga.order.notify-failure event so the notification service
         * sends a failure email to the customer.
         */
        void publishSagaNotifyFailure(String orderId, String userId, String email,
                        String orderReference, String amount, String currency, String reason);

        // ── CJ Fulfillment Events ────────────────────────────────────────────────

        /** Published when CJ payment is completed and order is being processed. */
        void publishCjOrderPaid(String orderId, String amount);

        /** Published when CJ balance is insufficient to pay for an order. */
        void publishCjOrderAwaitingFunds(String orderId, String needed, String available);

        /** Published when a pipeline step fails (for admin alerting). */
        void publishCjFulfillmentFailed(String orderId, String step, String reason);

        /** Published when CJ account balance drops below the configured threshold. */
        void publishCjBalanceLow(String balance, String threshold);
}
