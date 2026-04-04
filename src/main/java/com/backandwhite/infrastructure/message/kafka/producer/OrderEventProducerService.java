package com.backandwhite.infrastructure.message.kafka.producer;

import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.core.kafka.avro.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class OrderEventProducerService {

        private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

        // ── Order Events ─────────────────────────────────────────────────────────

        public void publishOrderCreated(String orderId, String userId, String email,
                        String orderReference, String totalAmount,
                        String status, int itemCount, String shippingAddressId) {
                var event = OrderCreatedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setTotalAmount(totalAmount)
                                .setCurrency("EUR")
                                .setStatus(status)
                                .setItemCount(itemCount)
                                .setShippingAddressId(shippingAddressId)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_CREATED, orderId, event);
        }

        public void publishOrderConfirmed(String orderId, String userId, String email,
                        String orderReference, String totalAmount, int itemCount) {
                var event = OrderConfirmedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setTotalAmount(totalAmount)
                                .setCurrency("EUR")
                                .setItemCount(itemCount)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_CONFIRMED, orderId, event);
        }

        public void publishOrderStatusUpdated(String orderId, String userId, String email,
                        String orderReference, String previousStatus,
                        String newStatus) {
                var event = OrderStatusUpdatedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setPreviousStatus(previousStatus)
                                .setNewStatus(newStatus)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_STATUS_UPDATED, orderId, event);
        }

        public void publishOrderCancelled(String orderId, String userId, String email,
                        String orderReference, String reason) {
                var event = OrderCancelledEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setReason(reason)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_CANCELLED, orderId, event);
        }

        public void publishOrderShipped(String orderId, String userId, String email,
                        String orderReference, String trackingNumber,
                        String carrier, String estimatedDelivery) {
                var event = OrderShippedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setTrackingNumber(trackingNumber)
                                .setCarrier(carrier)
                                .setEstimatedDelivery(estimatedDelivery)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_SHIPPED, orderId, event);
        }

        public void publishOrderDelivered(String orderId, String userId, String email,
                        String orderReference, String totalAmount) {
                var event = OrderDeliveredEvent.newBuilder()
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setTotalAmount(totalAmount)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_DELIVERED, orderId, event);
        }

        public void publishOrderReturnRequested(String orderId, String returnRequestId,
                        String userId, String email,
                        String orderReference, String reason) {
                var event = OrderReturnRequestedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setReturnRequestId(returnRequestId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setReason(reason)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_RETURN_REQUESTED, orderId, event);
        }

        public void publishOrderReturnApproved(String orderId, String returnRequestId,
                        String userId, String email,
                        String orderReference, String refundAmount) {
                var event = OrderReturnApprovedEvent.newBuilder()
                                .setOrderId(orderId)
                                .setReturnRequestId(returnRequestId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setOrderReference(orderReference)
                                .setRefundAmount(refundAmount)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_ORDER_RETURN_APPROVED, orderId, event);
        }

        // ── Cart Events ──────────────────────────────────────────────────────────

        public void publishCartAbandoned(String cartId, String userId, String email,
                        String totalAmount, int itemCount, String lastActivityAt) {
                var event = CartAbandonedEvent.newBuilder()
                                .setCartId(cartId)
                                .setUserId(userId != null ? userId : "anonymous")
                                .setEmail(email)
                                .setTotalAmount(totalAmount)
                                .setItemCount(itemCount)
                                .setLastActivityAt(lastActivityAt)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_CART_ABANDONED, cartId, event);
        }

        public void publishCartCheckoutInitiated(String cartId, String orderId, String userId,
                        String email, String totalAmount, int itemCount,
                        String couponCode, String shippingAddressId) {
                var event = CartCheckoutInitiatedEvent.newBuilder()
                                .setCartId(cartId)
                                .setOrderId(orderId)
                                .setUserId(userId)
                                .setEmail(email)
                                .setTotalAmount(totalAmount)
                                .setCurrency("EUR")
                                .setItemCount(itemCount)
                                .setCouponCode(couponCode)
                                .setShippingAddressId(shippingAddressId)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_CART_CHECKOUT_INITIATED, cartId, event);
        }

        // ── Stock Events (request to catalog) ────────────────────────────────────

        public void publishStockReservation(String productId, String variantId,
                        String orderId, int quantity) {
                var event = StockReservedEvent.newBuilder()
                                .setProductId(productId)
                                .setVariantId(variantId)
                                .setOrderId(orderId)
                                .setQuantity(quantity)
                                .setRemainingStock(0)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_STOCK_RESERVED, orderId, event);
        }

        public void publishStockDeducted(String productId, String variantId,
                        String orderId, int quantity) {
                var event = StockDeductedEvent.newBuilder()
                                .setProductId(productId)
                                .setVariantId(variantId)
                                .setOrderId(orderId)
                                .setQuantity(quantity)
                                .setRemainingStock(0)
                                .setTimestamp(now())
                                .build();
                send(AppConstants.KAFKA_TOPIC_STOCK_DEDUCTED, orderId, event);
        }

        // ── Common ───────────────────────────────────────────────────────────────

        private void send(String topic, String key, SpecificRecord event) {
                log.info("::> Publishing to [{}] key={}: {}", topic, key, event.getClass().getSimpleName());
                kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                        if (ex != null) {
                                log.error("::> Failed to publish to [{}]: {}", topic, ex.getMessage(), ex);
                        } else {
                                log.debug("::> Published to [{}] offset={}",
                                                topic, result.getRecordMetadata().offset());
                        }
                });
        }

        private String now() {
                return Instant.now().toString();
        }
}
