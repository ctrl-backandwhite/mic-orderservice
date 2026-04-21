package com.backandwhite.api.controller;

import com.backandwhite.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies Fase 1 of the CJ Dropshipping integration — webhook endpoints are
 * protected by a path-embedded secret. Requests with missing or wrong token
 * must return 404 so the endpoint remains invisible to probes that do not know
 * the shared secret.
 */
class CjWebhookControllerIT extends BaseIntegrationTest {

    private static final String VALID_SECRET = "test-webhook-secret-abc123";
    private static final String WRONG_SECRET = "intruder";

    private static final Map<String, Object> ORDER_PAYLOAD = Map.of("messageId", "msg-it-" + System.nanoTime(), "type",
            "ORDER_STATUS", "messageType", "ORDER_STATUS", "params", Map.of());

    @Test
    @DisplayName("order webhook with valid token returns 200 and CJ-expected body")
    void orderWebhook_validToken_200() {
        webTestClient.post().uri("/api/v1/cj/webhook/{token}/order", VALID_SECRET).bodyValue(ORDER_PAYLOAD).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.result").isEqualTo(true);
    }

    @Test
    @DisplayName("order webhook with wrong token returns 404")
    void orderWebhook_wrongToken_404() {
        webTestClient.post().uri("/api/v1/cj/webhook/{token}/order", WRONG_SECRET).bodyValue(ORDER_PAYLOAD).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("order webhook with empty token segment returns 404")
    void orderWebhook_emptyToken_404() {
        webTestClient.post().uri("/api/v1/cj/webhook/{token}/order", " ").bodyValue(ORDER_PAYLOAD).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("logistics webhook with valid token returns 200")
    void logisticsWebhook_validToken_200() {
        Map<String, Object> payload = Map.of("messageId", "msg-log-" + System.nanoTime(), "type", "LOGISTICS",
                "messageType", "LOGISTICS", "params", Map.of());

        webTestClient.post().uri("/api/v1/cj/webhook/{token}/logistics", VALID_SECRET).bodyValue(payload).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.result").isEqualTo(true);
    }

    @Test
    @DisplayName("logistics webhook with wrong token returns 404")
    void logisticsWebhook_wrongToken_404() {
        Map<String, Object> payload = Map.of("messageId", "msg-log-bad-" + System.nanoTime(), "type", "LOGISTICS",
                "messageType", "LOGISTICS", "params", Map.of());

        webTestClient.post().uri("/api/v1/cj/webhook/{token}/logistics", WRONG_SECRET).bodyValue(payload).exchange()
                .expectStatus().isNotFound();
    }
}
