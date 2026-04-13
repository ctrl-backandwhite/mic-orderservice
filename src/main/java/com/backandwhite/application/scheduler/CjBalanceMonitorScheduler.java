package com.backandwhite.application.scheduler;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Checks the CJ Dropshipping account balance every hour and publishes
 * a {@code cj.balance.low} Kafka event when it falls below the configured
 * threshold.
 */
@Log4j2
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cj.enabled", havingValue = "true", matchIfMissing = false)
public class CjBalanceMonitorScheduler {

    private final CjShoppingPort cjShoppingPort;
    private final OrderEventPort orderEventPort;

    @Value("${app.cj.min-balance-alert:50.00}")
    private BigDecimal minBalanceAlert;

    @Scheduled(cron = "${app.cj.balance-check-cron:0 0 * * * *}")
    public void checkBalance() {
        log.debug("══ CJ Balance Monitor ══");
        try {
            BigDecimal balance = cjShoppingPort.getBalanceAmount();
            if (balance == null) {
                log.warn("CJ balance unavailable — skipping threshold check");
                return;
            }
            log.info("CJ account balance: {} USD (threshold: {})", balance, minBalanceAlert);
            if (balance.compareTo(minBalanceAlert) < 0) {
                log.warn("CJ balance {} is below threshold {}! Publishing cj.balance.low event.", balance,
                        minBalanceAlert);
                orderEventPort.publishCjBalanceLow(balance.toPlainString(), minBalanceAlert.toPlainString());
            }
        } catch (Exception e) {
            log.error("CJ balance check failed: {}", e.getMessage(), e);
        }
    }
}
