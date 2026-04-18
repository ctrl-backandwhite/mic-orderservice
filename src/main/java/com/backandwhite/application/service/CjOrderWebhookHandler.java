package com.backandwhite.application.service;

import com.backandwhite.api.dto.webhook.CjOrderSplitWebhookParams;
import com.backandwhite.api.dto.webhook.CjOrderWebhookParams;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderSplitEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderSplitJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles ORDER and ORDERSPLIT webhook events from CJ Dropshipping.
 * <p>
 * Updates {@code cj_orders.cj_order_status}, sets {@code last_webhook_at},
 * copies tracking information when available, and records order-split events.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjOrderWebhookHandler {

    private final CjOrderRepository cjOrderRepository;
    private final CjOrderSplitJpaRepository splitRepository;

    @Transactional
    public void handleOrderEvent(String messageType, CjOrderWebhookParams params) {
        String cjOrderId = params.getOrderId();
        cjOrderRepository.findByCjOrderId(cjOrderId).ifPresentOrElse(order -> updateOrderFromWebhook(order, params),
                () -> log.warn("Received ORDER webhook for unknown cjOrderId={}", cjOrderId));
    }

    @Transactional
    public void handleOrderSplitEvent(CjOrderSplitWebhookParams params) {
        String originalCjOrderId = params.getOrderId();
        log.info("Processing ORDERSPLIT for cjOrderId={}", originalCjOrderId);

        cjOrderRepository.findByCjOrderId(originalCjOrderId).ifPresentOrElse(order -> {
            if (params.getSubOrders() != null) {
                for (CjOrderSplitWebhookParams.SubOrder sub : params.getSubOrders()) {
                    CjOrderSplitEntity split = CjOrderSplitEntity.builder().id(UUID.randomUUID().toString())
                            .orderId(order.getOrderId()).originalCjOrderId(originalCjOrderId)
                            .splitCjOrderId(sub.getCjOrderId()).orderStatus(sub.getOrderStatus())
                            .productList(sub.getProductList()).build();
                    splitRepository.save(split);
                    log.info("Saved split: originalCjOrderId={} -> splitCjOrderId={}", originalCjOrderId,
                            sub.getCjOrderId());
                }
            }
            order.setLastWebhookAt(Instant.now());
            order.setUpdatedBy("WEBHOOK");
            cjOrderRepository.save(order);
        }, () -> log.warn("Received ORDERSPLIT for unknown cjOrderId={}", originalCjOrderId));
    }

    private void updateOrderFromWebhook(CjOrder order, CjOrderWebhookParams params) {
        log.info("ORDER webhook: cjOrderId={} status={}", params.getOrderId(), params.getOrderStatus());

        mapStatus(params.getOrderStatus()).ifPresent(order::setCjOrderStatus);

        if (params.getTrackNumber() != null && !params.getTrackNumber().isBlank()) {
            order.setTrackNumber(params.getTrackNumber());
        }
        if (params.getLogisticName() != null && !params.getLogisticName().isBlank()) {
            order.setLogisticName(params.getLogisticName());
        }
        order.setLastWebhookAt(Instant.now());
        order.setUpdatedBy("WEBHOOK");
        cjOrderRepository.save(order);
    }

    private java.util.Optional<CjOrderStatus> mapStatus(String cjStatus) {
        if (cjStatus == null)
            return java.util.Optional.empty();
        return switch (cjStatus.toUpperCase()) {
            case "CREATED" -> java.util.Optional.of(CjOrderStatus.CREATED);
            case "IN_PRODUCTION" -> java.util.Optional.of(CjOrderStatus.IN_PRODUCTION);
            case "SHIPPED" -> java.util.Optional.of(CjOrderStatus.SHIPPED);
            case "DELIVERED" -> java.util.Optional.of(CjOrderStatus.DELIVERED);
            case "CANCELLED" -> java.util.Optional.of(CjOrderStatus.CANCELLED);
            default -> {
                log.debug("Unmapped CJ order status: {}", cjStatus);
                yield java.util.Optional.empty();
            }
        };
    }
}
