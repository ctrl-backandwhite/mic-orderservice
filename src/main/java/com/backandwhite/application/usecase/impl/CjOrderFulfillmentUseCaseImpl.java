package com.backandwhite.application.usecase.impl;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.CJ_ORDER_NOT_FOUND;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class CjOrderFulfillmentUseCaseImpl implements CjOrderFulfillmentUseCase {

    private final CjShoppingPort cjShoppingPort;
    private final CjOrderRepository cjOrderRepository;
    private final OrderUseCase orderUseCase;
    private final CjFulfillmentPipelineService pipelineService;

    /**
     * Global kill-switch. When {@code false}, {@link #submitOrderToCj} is a no-op —
     * useful for local testing that stops just before the saga fires. Leaves the
     * rest of the flow (ledger, snapshot, history) intact. Defaults to {@code true}
     * so plain unit tests (no Spring context, no {@code @Value} resolution) still
     * exercise the submit path.
     */
    @org.springframework.beans.factory.annotation.Value("${app.cj.enabled:true}")
    private boolean cjEnabled = true;

    @Override
    @Transactional
    public CjOrder submitOrderToCj(String orderId) {
        if (!cjEnabled) {
            log.warn("::> CJ DISABLED (app.cj.enabled=false) — skipping submit for orderId={}", orderId);
            return CjOrder.builder().orderId(orderId).cjOrderStatus(CjOrderStatus.UNPAID).fulfillmentStep("DISABLED")
                    .build();
        }
        log.info("::> Submitting order={} to CJ Dropshipping...", orderId);
        Order order = orderUseCase.findById(orderId);

        // Idempotency: skip if already submitted
        cjOrderRepository.findByOrderId(orderId).ifPresent(existing -> {
            if (existing.getCjOrderId() != null) {
                log.info("::> Order={} already submitted to CJ (cjOrderId={}), skipping.", orderId,
                        existing.getCjOrderId());
                throw new IllegalStateException("Order already submitted to CJ: " + orderId);
            }
        });

        CjOrder cjOrder = cjShoppingPort.createOrder(order);
        cjOrder.setId(UUID.randomUUID().toString());
        cjOrder.setOrderId(orderId);
        cjOrder.setCreatedBy("SYSTEM");

        CjOrder saved = cjOrderRepository.save(cjOrder);

        // Update order with cjOrderId
        if (saved.getCjOrderId() != null) {
            orderUseCase.updateCjFields(orderId, saved.getCjOrderId(), null);
        }

        log.info("::> Order={} submitted to CJ, cjOrderId={}", orderId, saved.getCjOrderId());

        // Kick off the fulfillment pipeline asynchronously (best-effort)
        try {
            pipelineService.processFulfillment(saved);
        } catch (Exception ex) {
            log.warn("::> Pipeline failed immediately after submit for orderId={}: {} (will retry via scheduler)",
                    orderId, ex.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public CjOrder syncStatus(String orderId) {
        CjOrder existing = cjOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> CJ_ORDER_NOT_FOUND.toBusinessException(orderId));

        if (existing.getCjOrderId() == null) {
            log.warn("::> Cannot sync order={}: no cjOrderId set.", orderId);
            return existing;
        }

        CjOrder fresh = cjShoppingPort.getOrderDetail(existing.getCjOrderId());
        existing.setCjOrderStatus(fresh.getCjOrderStatus());
        existing.setTrackNumber(fresh.getTrackNumber());
        existing.setLogisticName(fresh.getLogisticName());
        existing.setProductInfoList(fresh.getProductInfoList());
        existing.setLastSyncedAt(Instant.now());
        existing.setUpdatedBy("SCHEDULER");

        CjOrder updated = cjOrderRepository.save(existing);

        // Propagate status to order if CJ status changed
        if (fresh.getCjOrderStatus() != null) {
            OrderStatus internalStatus = fresh.getCjOrderStatus().toInternalStatus();
            if (internalStatus != null) {
                try {
                    orderUseCase.updateStatus(orderId, internalStatus, "CJ_SYNC",
                            "CJ status: " + fresh.getCjOrderStatus());
                } catch (Exception e) {
                    log.warn("::> Could not update internal order status for orderId={}: {}", orderId, e.getMessage());
                }
            }
        }

        // Propagate tracking to order
        if (fresh.getTrackNumber() != null) {
            orderUseCase.updateCjFields(orderId, existing.getCjOrderId(), fresh.getTrackNumber());
        }

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public CjOrder findByOrderId(String orderId) {
        return cjOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("CjOrder", orderId));
    }

    @Override
    @Transactional
    public void cancelCjOrder(String orderId) {
        CjOrder existing = cjOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> CJ_ORDER_NOT_FOUND.toBusinessException(orderId));

        if (existing.getCjOrderId() == null) {
            log.warn("::> Cannot cancel CJ order for orderId={}: no cjOrderId set.", orderId);
            return;
        }

        log.info("::> Cancelling CJ order for orderId={} cjOrderId={}", orderId, existing.getCjOrderId());
        cjShoppingPort.deleteOrder(existing.getCjOrderId());

        existing.setCjOrderStatus(CjOrderStatus.CANCELLED);
        existing.setUpdatedBy("SYSTEM");
        cjOrderRepository.save(existing);
        log.info("::> CJ order cancelled for orderId={}", orderId);
    }

}
