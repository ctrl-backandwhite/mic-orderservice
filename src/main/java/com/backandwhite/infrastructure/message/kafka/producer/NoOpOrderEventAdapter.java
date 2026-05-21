package com.backandwhite.infrastructure.message.kafka.producer;

import com.backandwhite.application.port.out.OrderEventPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback adapter wired when Kafka is disabled (local dev / tests).
 *
 * <p>
 * Every method is intentionally a no-op — the event is silently dropped so the
 * rest of the use-case flow keeps working without a broker. Production uses
 * {@link KafkaOrderEventAdapter} instead.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOrderEventAdapter implements OrderEventPort {

    @Override
    public void publishOrderCreated(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currencyCode, String status, int itemCount, String shippingAddressId) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderConfirmed(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currencyCode, String totalAmountUsd, int itemCount) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderStatusUpdated(String orderId, String userId, String email, String orderReference,
            String previousStatus, String newStatus) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderCancelled(String orderId, String userId, String email, String orderReference,
            String reason) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderShipped(String orderId, String userId, String email, String orderReference,
            String trackingNumber, String carrier, String estimatedDelivery) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderDelivered(String orderId, String userId, String email, String orderReference,
            String totalAmount) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderReturnRequested(String orderId, String returnRequestId, String userId, String email,
            String orderReference, String reason) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishOrderReturnApproved(String orderId, String returnRequestId, String userId, String email,
            String orderReference, String refundAmount) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCartAbandoned(String cartId, String userId, String email, String totalAmount, int itemCount,
            String lastActivityAt) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCartCheckoutInitiated(String cartId, String orderId, String userId, String email,
            String totalAmount, String currencyCode, int itemCount, String couponCode, String shippingAddressId) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishStockReservation(String productId, String variantId, String orderId, int quantity) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishStockDeducted(String productId, String variantId, String orderId, int quantity) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishInvoiceEmail(String email, String subject, String templateName,
            java.util.Map<String, String> variables) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishSagaPaymentRequested(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currency) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishSagaStockRelease(String orderId, String userId, String reason) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishSagaNotifyFailure(String orderId, String userId, String email, String orderReference,
            String amount, String currency, String reason) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCjOrderPaid(String orderId, String amount) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCjOrderAwaitingFunds(String orderId, String needed, String available) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCjFulfillmentFailed(String orderId, String step, String reason) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCjBalanceLow(String balance, String threshold) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCatalogProductUpdate(String pid, String rawPayload) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCatalogProductDelete(String pid) {
        // Intentionally empty: Kafka disabled, event dropped.
    }

    @Override
    public void publishCatalogStockChange(String vid, Integer remaining, String rawPayload) {
        // Intentionally empty: Kafka disabled, event dropped.
    }
}
