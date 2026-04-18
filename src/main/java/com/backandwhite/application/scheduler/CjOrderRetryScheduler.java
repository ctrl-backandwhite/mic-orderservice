package com.backandwhite.application.scheduler;

import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries CJ orders in three scenarios:
 * <ol>
 * <li>CREATED — order was never submitted to CJ (cjOrderId absent).</li>
 * <li>PIPELINE_FAILED — fulfillment pipeline stalled at an intermediate
 * step.</li>
 * <li>AWAITING_FUNDS — payment was deferred due to insufficient balance.</li>
 * </ol>
 * Runs every 30 minutes by default (configurable via
 * {@code app.cj.retry-cron}).
 */
@Log4j2
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cj.enabled", havingValue = "true", matchIfMissing = false)
public class CjOrderRetryScheduler {

    private static final int MAX_RETRIES = 5;

    private final CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    private final CjFulfillmentPipelineService pipelineService;
    private final CjOrderRepository cjOrderRepository;

    /** Retry CREATED orders (no cjOrderId yet). */
    @Scheduled(cron = "${app.cj.retry-cron:0 */30 * * * *}")
    public void retryFailedOrders() {
        log.info("══════ CJ Order Retry (CREATED) ══════");
        List<CjOrder> candidates = cjOrderRepository.findByStatusAndErrorCountLessThan(CjOrderStatus.CREATED,
                MAX_RETRIES);
        log.info("Found {} CREATED orders to retry", candidates.size());

        int success = 0;
        for (CjOrder cjOrder : candidates) {
            try {
                cjOrderFulfillmentUseCase.submitOrderToCj(cjOrder.getOrderId());
                success++;
            } catch (Exception e) {
                log.warn("::> Submit retry failed for orderId={}: {}", cjOrder.getOrderId(), e.getMessage());
                incrementErrorCount(cjOrder, e.getMessage());
            }
        }
        log.info("CREATED retry done: {}/{} succeeded", success, candidates.size());
        log.info("══════════════════════════════════════");
    }

    /**
     * Resume PIPELINE_FAILED and AWAITING_FUNDS orders via the fulfillment
     * pipeline.
     */
    @Scheduled(cron = "${app.cj.pipeline-retry-cron:0 */30 * * * *}")
    public void retryPipelineOrders() {
        log.info("══════ CJ Pipeline Retry (PIPELINE_FAILED / AWAITING_FUNDS) ══════");
        List<CjOrder> candidates = cjOrderRepository.findByStatusInAndErrorCountLessThan(
                List.of(CjOrderStatus.PIPELINE_FAILED, CjOrderStatus.AWAITING_FUNDS), MAX_RETRIES);
        log.info("Found {} pipeline-stalled orders to resume", candidates.size());

        int success = 0;
        for (CjOrder cjOrder : candidates) {
            try {
                pipelineService.resumeFulfillment(cjOrder);
                success++;
            } catch (Exception e) {
                log.warn("::> Pipeline resume failed for orderId={}: {}", cjOrder.getOrderId(), e.getMessage());
                incrementErrorCount(cjOrder, e.getMessage());
            }
        }
        log.info("Pipeline retry done: {}/{} succeeded", success, candidates.size());
        log.info("══════════════════════════════════════════════════════════════════");
    }

    private void incrementErrorCount(CjOrder cjOrder, String error) {
        try {
            cjOrder.setErrorCount(cjOrder.getErrorCount() + 1);
            cjOrder.setLastError(error);
            cjOrder.setUpdatedBy("SCHEDULER");
            cjOrderRepository.save(cjOrder);
        } catch (Exception e) {
            log.error("::> Failed to update error count for orderId={}: {}", cjOrder.getOrderId(), e.getMessage());
        }
    }
}
