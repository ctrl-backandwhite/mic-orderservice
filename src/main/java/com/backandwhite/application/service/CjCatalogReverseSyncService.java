package com.backandwhite.application.service;

import com.backandwhite.application.port.out.OrderEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Fase 12 — reacts to CJ PRODUCT / STOCK webhooks by emitting Kafka events so
 * the catalog service can re-apply the margin, reindex Elasticsearch, or flag
 * discontinued products. We don't mutate the catalog DB directly — that's
 * {@code mic-productcategory}'s job. We only translate CJ push into domain
 * events and forward them.
 *
 * <p>
 * Idempotency is already covered by {@code cj_webhook_log.message_id}; if the
 * same webhook arrives twice, the controller short-circuits before calling us.
 * </p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjCatalogReverseSyncService {

    private final OrderEventPort orderEventPort;

    public void onProductUpdate(String pid, String payload) {
        log.info("::> [Fase12] product update pid={}", pid);
        orderEventPort.publishCatalogProductUpdate(pid, payload);
    }

    public void onProductDelete(String pid) {
        log.info("::> [Fase12] product delete pid={}", pid);
        orderEventPort.publishCatalogProductDelete(pid);
    }

    public void onStockChange(String vid, Integer remaining, String payload) {
        log.info("::> [Fase12] stock vid={} remaining={}", vid, remaining);
        orderEventPort.publishCatalogStockChange(vid, remaining, payload);
    }
}
