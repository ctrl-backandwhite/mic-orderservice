package com.backandwhite.infrastructure.message.kafka.consumer;

import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.core.kafka.avro.PaymentConfirmedEvent;
import com.backandwhite.core.kafka.avro.PaymentFailedEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderDeliveredEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderShippedEvent;
import com.backandwhite.domain.valureobject.OrderStatus;
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

    private final OrderUseCase orderUseCase;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_PAYMENT_CONFIRMED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received payment.confirmed: orderId={}, paymentId={}, amount={}",
                orderId, str(event.getPaymentId()), str(event.getAmount()));
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");
        } catch (Exception e) {
            log.error("::> Failed processing payment.confirmed for order={}: {}",
                    orderId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_PAYMENT_FAILED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentFailed(PaymentFailedEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received payment.failed: orderId={}, reason={}",
                orderId, str(event.getReason()));
        try {
            orderUseCase.cancel(orderId, str(event.getUserId()),
                    "Payment failed: " + str(event.getReason()));
        } catch (Exception e) {
            log.error("::> Failed processing payment.failed for order={}: {}",
                    orderId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_SHIPPING_ORDER_SHIPPED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onShippingOrderShipped(ShippingOrderShippedEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received shipping.order.shipped: orderId={}, tracking={}",
                orderId, str(event.getTrackingNumber()));
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.SHIPPED, "SYSTEM",
                    "Shipped via " + str(event.getCarrier()) + " tracking: " + str(event.getTrackingNumber()));
        } catch (Exception e) {
            log.error("::> Failed processing shipping.order.shipped for order={}: {}",
                    orderId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_SHIPPING_ORDER_DELIVERED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onShippingOrderDelivered(ShippingOrderDeliveredEvent event) {
        String orderId = str(event.getOrderId());
        log.info("::> Received shipping.order.delivered: orderId={}", orderId);
        try {
            orderUseCase.updateStatus(orderId, OrderStatus.DELIVERED, "SYSTEM",
                    "Delivered at " + str(event.getDeliveredAt()));
        } catch (Exception e) {
            log.error("::> Failed processing shipping.order.delivered for order={}: {}",
                    orderId, e.getMessage(), e);
        }
    }

    private String str(CharSequence cs) {
        return cs != null ? cs.toString() : null;
    }
}
