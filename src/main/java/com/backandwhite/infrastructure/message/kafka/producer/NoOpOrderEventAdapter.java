package com.backandwhite.infrastructure.message.kafka.producer;

import com.backandwhite.application.port.out.OrderEventPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOrderEventAdapter implements OrderEventPort {

        @Override
        public void publishOrderCreated(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currencyCode,
                        String status, int itemCount, String shippingAddressId) {
        }

        @Override
        public void publishOrderConfirmed(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currencyCode, int itemCount) {
        }

        @Override
        public void publishOrderStatusUpdated(String orderId, String userId, String email,
                        String orderReference, String previousStatus, String newStatus) {
        }

        @Override
        public void publishOrderCancelled(String orderId, String userId, String email,
                        String orderReference, String reason) {
        }

        @Override
        public void publishOrderShipped(String orderId, String userId, String email,
                        String orderReference, String trackingNumber,
                        String carrier, String estimatedDelivery) {
        }

        @Override
        public void publishOrderDelivered(String orderId, String userId, String email,
                        String orderReference, String totalAmount) {
        }

        @Override
        public void publishOrderReturnRequested(String orderId, String returnRequestId,
                        String userId, String email,
                        String orderReference, String reason) {
        }

        @Override
        public void publishOrderReturnApproved(String orderId, String returnRequestId,
                        String userId, String email,
                        String orderReference, String refundAmount) {
        }

        @Override
        public void publishCartAbandoned(String cartId, String userId, String email,
                        String totalAmount, int itemCount, String lastActivityAt) {
        }

        @Override
        public void publishCartCheckoutInitiated(String cartId, String orderId, String userId,
                        String email, String totalAmount, String currencyCode, int itemCount,
                        String couponCode, String shippingAddressId) {
        }

        @Override
        public void publishStockReservation(String productId, String variantId,
                        String orderId, int quantity) {
        }

        @Override
        public void publishStockDeducted(String productId, String variantId,
                        String orderId, int quantity) {
        }

        @Override
        public void publishInvoiceEmail(String email, String subject, String templateName,
                        java.util.Map<String, String> variables) {
        }

        @Override
        public void publishSagaPaymentRequested(String orderId, String userId, String email,
                        String orderReference, String totalAmount, String currency) {
        }

        @Override
        public void publishSagaStockRelease(String orderId, String userId, String reason) {
        }

        @Override
        public void publishSagaNotifyFailure(String orderId, String userId, String email,
                        String orderReference, String amount, String currency, String reason) {
        }

        @Override
        public void publishCjOrderPaid(String orderId, String amount) {
        }

        @Override
        public void publishCjOrderAwaitingFunds(String orderId, String needed, String available) {
        }

        @Override
        public void publishCjFulfillmentFailed(String orderId, String step, String reason) {
        }

        @Override
        public void publishCjBalanceLow(String balance, String threshold) {
        }
}
