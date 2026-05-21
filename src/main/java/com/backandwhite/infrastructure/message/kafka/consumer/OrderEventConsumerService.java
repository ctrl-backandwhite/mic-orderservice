package com.backandwhite.infrastructure.message.kafka.consumer;

import com.backandwhite.application.service.OrderCompensationService;
import com.backandwhite.application.service.OrderPaymentReconciliationService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.core.kafka.avro.PaymentConfirmedEvent;
import com.backandwhite.core.kafka.avro.PaymentFailedEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderDeliveredEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderShippedEvent;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes payment and shipping events to update order status.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class OrderEventConsumerService {

    private static final String CHANGED_BY_SYSTEM = "SYSTEM";

    private final OrderUseCase orderUseCase;
    private final OrderCompensationService orderCompensationService;
    private final CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    private final OrderPaymentReconciliationService reconciliationService;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_PAYMENT_CONFIRMED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        String orderId = str(event.getOrderId());
        String paymentId = str(event.getPaymentId());
        log.info("::> Received payment.confirmed: orderId={}, paymentId={}, amount={}", orderId, paymentId,
                str(event.getAmount()));
        try {
            // Walk the allowed ladder DRAFT → PENDING → CONFIRMED. The frontend
            // also calls confirmOrder (DRAFT → PENDING) in parallel, so here
            // we nudge the state forward without assuming where it is.
            advanceDraftToPending(orderId);
            advancePendingToConfirmed(orderId);

            // Fase 5 + 8.3 — ledger INBOUND + item snapshot + margin gate
            boolean marginAcceptable = runReconciliation(event, orderId, paymentId);
            if (!marginAcceptable) {
                log.warn("::> Order {} flagged NEEDS_REVIEW — NOT submitting to CJ", orderId);
                return;
            }

            // Submit to CJ Dropshipping only if reconciliation passed. The use
            // case itself honours app.cj.enabled and short-circuits when the
            // feature flag is off, so we don't need to gate again here.
            submitToCjSafely(orderId);
        } catch (Exception e) {
            log.error("::> Failed processing payment.confirmed for order={}: {}", orderId, e.getMessage(), e);
        }
    }

    private void advanceDraftToPending(String orderId) {
        try {
            var current = orderUseCase.findById(orderId);
            if (current != null && current.getStatus() == OrderStatus.DRAFT) {
                orderUseCase.updateStatus(orderId, OrderStatus.PENDING, CHANGED_BY_SYSTEM,
                        "Payment confirmed async — advancing DRAFT→PENDING");
            }
        } catch (Exception step1) {
            log.debug("::> DRAFT→PENDING skipped: {}", step1.getMessage());
        }
    }

    private void advancePendingToConfirmed(String orderId) {
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.CONFIRMED, CHANGED_BY_SYSTEM, "Payment confirmed");
        } catch (Exception step2) {
            // Idempotent — may already be CONFIRMED/PROCESSING by another path.
            log.debug("::> PENDING→CONFIRMED skipped: {}", step2.getMessage());
        }
    }

    private void submitToCjSafely(String orderId) {
        try {
            cjOrderFulfillmentUseCase.submitOrderToCj(orderId);
        } catch (Exception cjEx) {
            log.error("::> CJ submission failed for order={}: {} (will retry via scheduler)", orderId,
                    cjEx.getMessage());
        }
    }

    private boolean runReconciliation(PaymentConfirmedEvent event, String orderId, String paymentId) {
        try {
            BigDecimal amount = new BigDecimal(str(event.getAmount()));
            return reconciliationService.onPaymentConfirmed(orderId, paymentId, amount, str(event.getCurrency()),
                    str(event.getGateway()), str(event.getTransactionRef()));
        } catch (Exception reconEx) {
            log.error("::> Reconciliation failed for order={}: {} — failing safe (skip CJ submit)", orderId,
                    reconEx.getMessage(), reconEx);
            return false;
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_PAYMENT_FAILED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentFailed(PaymentFailedEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received payment.failed: orderId={}, reason={}", orderId, str(event.getReason()));
        try {
            orderUseCase.cancel(orderId, str(event.getUserId()), "Payment failed: " + str(event.getReason()));
            // Trigger Saga compensation: release stock + notify customer
            orderCompensationService.compensate(orderId, str(event.getUserId()), str(event.getEmail()), orderId, // orderReference
                                                                                                                 // falls
                                                                                                                 // back
                                                                                                                 // to
                                                                                                                 // orderId
                    str(event.getAmount()), "USD", str(event.getReason()));
        } catch (Exception e) {
            log.error("::> Failed processing payment.failed for order={}: {}", orderId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_SHIPPING_ORDER_SHIPPED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onShippingOrderShipped(ShippingOrderShippedEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received shipping.order.shipped: orderId={}, tracking={}", orderId,
                str(event.getTrackingNumber()));
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.SHIPPED, CHANGED_BY_SYSTEM,
                    "Shipped via " + str(event.getCarrier()) + " tracking: " + str(event.getTrackingNumber()));
        } catch (Exception e) {
            log.error("::> Failed processing shipping.order.shipped for order={}: {}", orderId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_SHIPPING_ORDER_DELIVERED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onShippingOrderDelivered(ShippingOrderDeliveredEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received shipping.order.delivered: orderId={}", orderId);
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.DELIVERED, CHANGED_BY_SYSTEM,
                    "Delivered at " + str(event.getDeliveredAt()));
        } catch (Exception e) {
            log.error("::> Failed processing shipping.order.delivered for order={}: {}", orderId, e.getMessage(), e);
        }
    }

    private String str(CharSequence cs) {
        return cs != null ? cs.toString() : null;
    }
}
