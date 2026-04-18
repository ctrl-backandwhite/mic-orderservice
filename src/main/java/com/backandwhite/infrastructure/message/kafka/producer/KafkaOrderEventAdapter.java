package com.backandwhite.infrastructure.message.kafka.producer;

import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.core.kafka.avro.*;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class KafkaOrderEventAdapter implements OrderEventPort {

    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    // ── Order Events ─────────────────────────────────────────────────────────

    public void publishOrderCreated(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currencyCode, String status, int itemCount, String shippingAddressId) {
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder().setOrderId(orderId).setUserId(userId).setEmail(email)
                .setOrderReference(orderReference).setTotalAmount(totalAmount)
                .setCurrency(currencyCode != null ? currencyCode : "USD").setStatus(status).setItemCount(itemCount)
                .setShippingAddressId(shippingAddressId).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_CREATED, orderId, event);
    }

    public void publishOrderConfirmed(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currencyCode, int itemCount) {
        OrderConfirmedEvent event = OrderConfirmedEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setTotalAmount(totalAmount)
                .setCurrency(currencyCode != null ? currencyCode : "USD").setItemCount(itemCount).setTimestamp(now())
                .build();
        send(AppConstants.KAFKA_TOPIC_ORDER_CONFIRMED, orderId, event);
    }

    public void publishOrderStatusUpdated(String orderId, String userId, String email, String orderReference,
            String previousStatus, String newStatus) {
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setPreviousStatus(previousStatus)
                .setNewStatus(newStatus).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_STATUS_UPDATED, orderId, event);
    }

    public void publishOrderCancelled(String orderId, String userId, String email, String orderReference,
            String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setReason(reason).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_CANCELLED, orderId, event);
    }

    public void publishOrderShipped(String orderId, String userId, String email, String orderReference,
            String trackingNumber, String carrier, String estimatedDelivery) {
        OrderShippedEvent event = OrderShippedEvent.newBuilder().setOrderId(orderId).setUserId(userId).setEmail(email)
                .setOrderReference(orderReference).setTrackingNumber(trackingNumber).setCarrier(carrier)
                .setEstimatedDelivery(estimatedDelivery).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_SHIPPED, orderId, event);
    }

    public void publishOrderDelivered(String orderId, String userId, String email, String orderReference,
            String totalAmount) {
        OrderDeliveredEvent event = OrderDeliveredEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setTotalAmount(totalAmount).setTimestamp(now())
                .build();
        send(AppConstants.KAFKA_TOPIC_ORDER_DELIVERED, orderId, event);
    }

    public void publishOrderReturnRequested(String orderId, String returnRequestId, String userId, String email,
            String orderReference, String reason) {
        OrderReturnRequestedEvent event = OrderReturnRequestedEvent.newBuilder().setOrderId(orderId)
                .setReturnRequestId(returnRequestId).setUserId(userId).setEmail(email).setOrderReference(orderReference)
                .setReason(reason).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_RETURN_REQUESTED, orderId, event);
    }

    public void publishOrderReturnApproved(String orderId, String returnRequestId, String userId, String email,
            String orderReference, String refundAmount) {
        OrderReturnApprovedEvent event = OrderReturnApprovedEvent.newBuilder().setOrderId(orderId)
                .setReturnRequestId(returnRequestId).setUserId(userId).setEmail(email).setOrderReference(orderReference)
                .setRefundAmount(refundAmount).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_ORDER_RETURN_APPROVED, orderId, event);
    }

    // ── Cart Events ──────────────────────────────────────────────────────────

    public void publishCartAbandoned(String cartId, String userId, String email, String totalAmount, int itemCount,
            String lastActivityAt) {
        CartAbandonedEvent event = CartAbandonedEvent.newBuilder().setCartId(cartId)
                .setUserId(userId != null ? userId : "anonymous").setEmail(email).setTotalAmount(totalAmount)
                .setItemCount(itemCount).setLastActivityAt(lastActivityAt).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CART_ABANDONED, cartId, event);
    }

    public void publishCartCheckoutInitiated(String cartId, String orderId, String userId, String email,
            String totalAmount, String currencyCode, int itemCount, String couponCode, String shippingAddressId) {
        CartCheckoutInitiatedEvent event = CartCheckoutInitiatedEvent.newBuilder().setCartId(cartId).setOrderId(orderId)
                .setUserId(userId).setEmail(email).setTotalAmount(totalAmount)
                .setCurrency(currencyCode != null ? currencyCode : "USD").setItemCount(itemCount)
                .setCouponCode(couponCode).setShippingAddressId(shippingAddressId).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CART_CHECKOUT_INITIATED, cartId, event);
    }

    // ── Stock Events (request to catalog) ────────────────────────────────────

    public void publishStockReservation(String productId, String variantId, String orderId, int quantity) {
        StockReservedEvent event = StockReservedEvent.newBuilder().setProductId(productId).setVariantId(variantId)
                .setOrderId(orderId).setQuantity(quantity).setRemainingStock(0).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_STOCK_RESERVED, orderId, event);
    }

    public void publishStockDeducted(String productId, String variantId, String orderId, int quantity) {
        StockDeductedEvent event = StockDeductedEvent.newBuilder().setProductId(productId).setVariantId(variantId)
                .setOrderId(orderId).setQuantity(quantity).setRemainingStock(0).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_STOCK_DEDUCTED, orderId, event);
    }

    // ── Notification Events ───────────────────────────────────────────────────

    @Override
    public void publishInvoiceEmail(String email, String subject, String templateName, Map<String, String> variables) {
        if (email == null || email.isBlank()) {
            log.warn("::> Cannot publish invoice email: email is null/blank");
            return;
        }
        EmailNotificationEvent event = EmailNotificationEvent.newBuilder().setRecipient(email).setSubject(subject)
                .setTemplateName(templateName).setVariables(variables).build();
        send(AppConstants.KAFKA_TOPIC_NOTIFICATION_EMAIL, email, event);
    }

    // ── Saga Events (Compensation) ────────────────────────────────────────────

    @Override
    public void publishSagaPaymentRequested(String orderId, String userId, String email, String orderReference,
            String totalAmount, String currency) {
        SagaPaymentRequestedEvent event = SagaPaymentRequestedEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setTotalAmount(totalAmount).setCurrency(currency)
                .setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_SAGA_ORDER_PAYMENT_REQUESTED, orderId, event);
    }

    @Override
    public void publishSagaStockRelease(String orderId, String userId, String reason) {
        SagaStockReleaseEvent event = SagaStockReleaseEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setReason(reason).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_SAGA_ORDER_STOCK_RELEASE, orderId, event);
    }

    @Override
    public void publishSagaNotifyFailure(String orderId, String userId, String email, String orderReference,
            String amount, String currency, String reason) {
        SagaNotifyFailureEvent event = SagaNotifyFailureEvent.newBuilder().setOrderId(orderId).setUserId(userId)
                .setEmail(email).setOrderReference(orderReference).setAmount(amount).setCurrency(currency)
                .setReason(reason).setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_SAGA_ORDER_NOTIFY_FAILURE, orderId, event);
    }

    // ── CJ Fulfillment Events ─────────────────────────────────────────────────

    @Override
    public void publishCjOrderPaid(String orderId, String amount) {
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.newBuilder().setOrderId(orderId).setUserId("SYSTEM")
                .setEmail(null).setOrderReference(amount).setPreviousStatus("PIPELINE").setNewStatus("CJ_PAID")
                .setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CJ_ORDER_PAID, orderId, event);
    }

    @Override
    public void publishCjOrderAwaitingFunds(String orderId, String needed, String available) {
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.newBuilder().setOrderId(orderId).setUserId(needed)
                .setEmail(available).setOrderReference(orderId).setPreviousStatus("PIPELINE")
                .setNewStatus("CJ_AWAITING_FUNDS").setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CJ_ORDER_AWAITING_FUNDS, orderId, event);
    }

    @Override
    public void publishCjFulfillmentFailed(String orderId, String step, String reason) {
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.newBuilder().setOrderId(orderId).setUserId("SYSTEM")
                .setEmail(null).setOrderReference(step).setPreviousStatus(step).setNewStatus("CJ_PIPELINE_FAILED")
                .setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CJ_FULFILLMENT_FAILED, orderId, event);
    }

    @Override
    public void publishCjBalanceLow(String balance, String threshold) {
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.newBuilder().setOrderId("BALANCE_MONITOR")
                .setUserId("SYSTEM").setEmail(null).setOrderReference(balance).setPreviousStatus(balance)
                .setNewStatus("CJ_BALANCE_LOW").setTimestamp(now()).build();
        send(AppConstants.KAFKA_TOPIC_CJ_BALANCE_LOW, "BALANCE_MONITOR", event);
    }

    // ── Common ───────────────────────────────────────────────────────────────

    private void send(String topic, String key, SpecificRecord event) {
        log.info("::> Publishing to [{}] key={}: {}", topic, key, event.getClass().getSimpleName());
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("::> Failed to publish to [{}]: {}", topic, ex.getMessage(), ex);
            } else {
                log.debug("::> Published to [{}] offset={}", topic, result.getRecordMetadata().offset());
            }
        });
    }

    private String now() {
        return Instant.now().toString();
    }
}
