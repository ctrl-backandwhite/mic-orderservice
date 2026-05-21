package com.backandwhite.application.scheduler;

import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically syncs CJ order status and tracking information. Orders that
 * received a webhook within the last 2 hours are skipped — their status is
 * already up-to-date. Runs every 15 minutes by default (configurable via
 * app.cj.status-sync-cron).
 */
@Log4j2
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cj.enabled", havingValue = "true", matchIfMissing = false)
public class CjOrderStatusSyncScheduler {

    private final CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    private final CjOrderRepository cjOrderRepository;

    @Value("${app.cj.sync-batch-size:20}")
    private int batchSize;

    // Fase 7 — poll every 10 minutes and re-sync any non-terminal order that
    // hasn't been touched by a webhook in > 30 minutes. CJ drops webhooks often
    // enough that we can't rely on them alone.
    @Scheduled(cron = "${app.cj.status-sync-cron:0 */10 * * * *}")
    public void syncPendingOrders() {
        log.info("══════ CJ Order Status Sync ══════");
        List<CjOrder> pending = cjOrderRepository.findPendingSync(batchSize);
        log.info("Found {} CJ orders pending sync", pending.size());

        Instant thirtyMinAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        int skipped = 0;
        int success = 0;
        int failed = 0;
        for (CjOrder cjOrder : pending) {
            // Skip only orders whose webhook update is under 30 min old.
            if (cjOrder.getLastWebhookAt() != null && cjOrder.getLastWebhookAt().isAfter(thirtyMinAgo)) {
                log.debug("::> Skipping orderId={} — webhook received at {}", cjOrder.getOrderId(),
                        cjOrder.getLastWebhookAt());
                skipped++;
                continue;
            }
            try {
                cjOrderFulfillmentUseCase.syncStatus(cjOrder.getOrderId());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("::> Sync failed for orderId={}: {}", cjOrder.getOrderId(), e.getMessage());
                incrementErrorCount(cjOrder, e.getMessage());
            }
        }
        log.info("CJ Status Sync complete: {} success, {} failed, {} skipped (recent webhook)", success, failed,
                skipped);
        log.info("══════════════════════════════════");
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
