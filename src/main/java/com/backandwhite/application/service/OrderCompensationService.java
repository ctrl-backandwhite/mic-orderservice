package com.backandwhite.application.service;

import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.domain.valueobject.OrderSagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Orchestrates Saga compensation steps when an order's payment fails.
 * <p>
 * Choreography flow:
 * <ol>
 * <li>Update order sagaStatus to COMPENSATING</li>
 * <li>Publish saga.order.stock-release → inventory service releases reserved
 * stock</li>
 * <li>Publish saga.order.notify-failure → notification service emails the
 * customer</li>
 * <li>Update order sagaStatus to CANCELLED</li>
 * </ol>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OrderCompensationService {

    private final OrderEventPort orderEventPort;
    private final OrderUseCase orderUseCase;

    public void compensate(String orderId, String userId, String email, String orderReference, String amount,
            String currency, String reason) {

        log.info("::> [Saga] Starting compensation for orderId={}, reason={}", orderId, reason);

        try {
            orderUseCase.updateSagaStatus(orderId, OrderSagaStatus.COMPENSATING);
        } catch (Exception e) {
            log.warn("::> [Saga] Could not set COMPENSATING status for orderId={}: {}", orderId, e.getMessage());
        }

        try {
            orderEventPort.publishSagaStockRelease(orderId, userId, "payment-failed");
            log.info("::> [Saga] Published stock-release for orderId={}", orderId);
        } catch (Exception e) {
            log.error("::> [Saga] Failed to publish stock-release for orderId={}: {}", orderId, e.getMessage(), e);
        }

        try {
            orderEventPort.publishSagaNotifyFailure(orderId, userId, email, orderReference, amount, currency, reason);
            log.info("::> [Saga] Published notify-failure for orderId={}", orderId);
        } catch (Exception e) {
            log.error("::> [Saga] Failed to publish notify-failure for orderId={}: {}", orderId, e.getMessage(), e);
        }

        try {
            orderUseCase.updateSagaStatus(orderId, OrderSagaStatus.CANCELLED);
        } catch (Exception e) {
            log.warn("::> [Saga] Could not set CANCELLED saga status for orderId={}: {}", orderId, e.getMessage());
        }
    }
}
