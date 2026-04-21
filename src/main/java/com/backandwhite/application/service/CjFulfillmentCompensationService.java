package com.backandwhite.application.service;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Fase 6 — detects irrecoverable CJ errors and compensates: cancels the CJ
 * order, marks the local one as FULFILLMENT_FAILED and fires a refund event.
 * Balance-insufficient (1604000) is special-cased as AWAITING_FUNDS so the
 * scheduler doesn't keep retrying.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjFulfillmentCompensationService {

    /** Errors where retrying will never help — compensate immediately. */
    public static final Set<String> IRRECOVERABLE_CODES = Set.of("1602001", "1602003", "1605001");
    public static final String BALANCE_INSUFFICIENT = "1604000";

    private final CjShoppingPort cjShoppingPort;
    private final CjOrderRepository cjOrderRepository;
    private final OrderEventPort orderEventPort;
    private final OrderStateHistoryService stateHistoryService;

    public boolean handlePipelineFailure(CjOrder cjOrder, String step, String cjCode, String errorMessage) {
        if (cjCode == null) {
            return false;
        }

        if (BALANCE_INSUFFICIENT.equals(cjCode)) {
            log.warn("::> [Compensation] CJ balance insufficient — marking orderId={} AWAITING_FUNDS",
                    cjOrder.getOrderId());
            cjOrder.setCjOrderStatus(CjOrderStatus.UNPAID);
            cjOrder.setFulfillmentStep("AWAITING_FUNDS");
            cjOrder.setFulfillmentError("CJ balance insufficient at step " + step);
            cjOrderRepository.save(cjOrder);
            stateHistoryService.recordCjTransition(cjOrder.getCjOrderId(), cjOrder.getOrderId(), step, "AWAITING_FUNDS",
                    OrderStateHistoryService.ACTOR_SYSTEM, "CJ balance insufficient", null);
            orderEventPort.publishSagaNotifyFailure(cjOrder.getOrderId(), null, null, cjOrder.getOrderId(), "0", "USD",
                    "CJ_BALANCE_INSUFFICIENT");
            return true;
        }

        if (IRRECOVERABLE_CODES.contains(cjCode)) {
            log.error("::> [Compensation] irrecoverable CJ error {} at step {} — compensating orderId={}", cjCode, step,
                    cjOrder.getOrderId());
            try {
                cjShoppingPort.deleteOrder(cjOrder.getCjOrderId());
            } catch (Exception e) {
                log.warn("::> [Compensation] deleteOrder failed (non-fatal): {}", e.getMessage());
            }
            cjOrder.setCjOrderStatus(CjOrderStatus.CANCELLED);
            cjOrder.setFulfillmentStep("FULFILLMENT_FAILED");
            cjOrder.setFulfillmentError("[" + cjCode + "] " + errorMessage);
            cjOrderRepository.save(cjOrder);
            stateHistoryService.recordCjTransition(cjOrder.getCjOrderId(), cjOrder.getOrderId(), step,
                    "FULFILLMENT_FAILED", OrderStateHistoryService.ACTOR_SYSTEM, "Irrecoverable error " + cjCode,
                    errorMessage);
            // Trigger refund to customer — saga-notify-failure is the existing channel
            orderEventPort.publishSagaNotifyFailure(cjOrder.getOrderId(), null, null, cjOrder.getOrderId(), "0", "USD",
                    "CJ_FULFILLMENT_FAILED:" + cjCode);
            return true;
        }

        return false;
    }
}
