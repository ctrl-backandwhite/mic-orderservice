package com.backandwhite.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fase 17 — Micrometer facade so the rest of the app does not depend on
 * specific metric names. Every counter / histogram defined in the plan lives
 * here so all instrumentation is in one place.
 */
@Service
@RequiredArgsConstructor
public class CjMetricsService {

    private final MeterRegistry meterRegistry;

    private final AtomicReference<Double> balanceGauge = new AtomicReference<>(0.0);

    /** Count calls to CJ API by endpoint + cj code. */
    public void recordApiCall(String endpoint, String cjCode) {
        Counter.builder("cj_api_calls_total").tags(Tags.of("endpoint", safe(endpoint), "code", safe(cjCode)))
                .register(meterRegistry).increment();
    }

    /** Histogram of CJ API call latency. */
    public void recordApiLatency(String endpoint, Duration latency) {
        Timer.builder("cj_api_latency_seconds").tags(Tags.of("endpoint", safe(endpoint))).register(meterRegistry)
                .record(latency);
    }

    /** Histogram per pipeline step with outcome tag. */
    public void recordPipelineStep(String step, String outcome, Duration latency) {
        Timer.builder("cj_pipeline_step_duration_seconds").tags(Tags.of("step", safe(step), "outcome", safe(outcome)))
                .register(meterRegistry).record(latency);
    }

    /** Count webhooks processed by type. */
    public void recordWebhookEvent(String type, String messageType, String outcome) {
        Counter.builder("cj_webhook_events_total")
                .tags(Tags.of("type", safe(type), "messageType", safe(messageType), "outcome", safe(outcome)))
                .register(meterRegistry).increment();
    }

    /** Gauge of CJ account balance in USD. */
    public void updateBalance(double balanceUsd) {
        balanceGauge.set(balanceUsd);
        meterRegistry.gauge("cj_balance_usd", balanceGauge, AtomicReference::get);
    }

    /** Count irrecoverable fulfillment failures (plan Fase 6). */
    public void recordFulfillmentFailed(String cjCode, String step) {
        Counter.builder("orders_fulfillment_failed_total").tags(Tags.of("code", safe(cjCode), "step", safe(step)))
                .register(meterRegistry).increment();
    }

    private static String safe(String v) {
        return v == null ? "unknown" : v;
    }
}
