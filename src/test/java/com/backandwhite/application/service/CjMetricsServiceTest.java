package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CjMetricsService")
class CjMetricsServiceTest {

    private MeterRegistry registry;
    private CjMetricsService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new CjMetricsService(registry);
    }

    @Test
    @DisplayName("recordApiCall registers and increments the counter")
    void recordApiCall() {
        service.recordApiCall("/order/createOrder", "200000");
        double v = registry.counter("cj_api_calls_total", "endpoint", "/order/createOrder", "code", "200000").count();
        assertThat(v).isEqualTo(1.0);
    }

    @Test
    @DisplayName("null tags are normalised to 'unknown'")
    void recordApiCallNullTags() {
        service.recordApiCall(null, null);
        double v = registry.counter("cj_api_calls_total", "endpoint", "unknown", "code", "unknown").count();
        assertThat(v).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordApiLatency registers a timer and records the duration")
    void recordApiLatency() {
        service.recordApiLatency("/x", Duration.ofMillis(50));
        assertThat(registry.timer("cj_api_latency_seconds", "endpoint", "/x").count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recordPipelineStep tags step + outcome")
    void recordPipelineStep() {
        service.recordPipelineStep("ADD_CART", "OK", Duration.ofMillis(10));
        assertThat(registry.timer("cj_pipeline_step_duration_seconds", "step", "ADD_CART", "outcome", "OK").count())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("recordWebhookEvent increments per (type, messageType, outcome)")
    void recordWebhookEvent() {
        service.recordWebhookEvent("ORDER", "ORDER_PAID", "PROCESSED");
        assertThat(registry.counter("cj_webhook_events_total", "type", "ORDER", "messageType", "ORDER_PAID", "outcome",
                "PROCESSED").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("updateBalance registers a gauge and exposes the latest value")
    void updateBalance() {
        service.updateBalance(123.45);
        Gauge g = registry.find("cj_balance_usd").gauge();
        assertThat(g).isNotNull();
        assertThat(g.value()).isEqualTo(123.45);
    }

    @Test
    @DisplayName("recordFulfillmentFailed counts irrecoverable errors")
    void recordFulfillmentFailed() {
        service.recordFulfillmentFailed("1602001", "ADD_CART");
        assertThat(registry.counter("orders_fulfillment_failed_total", "code", "1602001", "step", "ADD_CART").count())
                .isEqualTo(1.0);
    }
}
