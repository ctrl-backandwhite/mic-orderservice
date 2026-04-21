package com.backandwhite.api.controller;

import com.backandwhite.api.dto.webhook.*;
import com.backandwhite.application.service.CjLogisticWebhookHandler;
import com.backandwhite.application.service.CjOrderWebhookHandler;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.infrastructure.configuration.CjWebhookProperties;
import com.backandwhite.infrastructure.db.postgres.entity.CjWebhookLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjWebhookLogJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives push notifications (webhooks) sent by CJ Dropshipping.
 * <p>
 * CJ does not sign requests with HMAC. Authentication is based on an opaque
 * rotable token embedded in the URL path
 * ({@code /api/v1/cj/webhook/{token}/…}). Requests whose token does not match
 * the configured {@code app.cj.webhook.secret} are rejected with
 * {@code 404 Not Found} — the goal is to keep the endpoint invisible to probes
 * that do not already know the secret.
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cj/webhook")
@Tag(name = "CJ Webhooks", description = "Webhook receivers for CJ Dropshipping push events")
public class CjWebhookController {

    private final CjWebhookLogJpaRepository webhookLogRepository;
    private final CjOrderWebhookHandler orderWebhookHandler;
    private final CjLogisticWebhookHandler logisticWebhookHandler;
    private final ObjectMapper objectMapper;
    private final CjWebhookProperties webhookProperties;

    /** CJ-required success response. */
    private static final Map<String, Object> OK = Map.of("result", true);

    // ─── ORDER + ORDERSPLIT ───────────────────────────────────────────────────

    @NxPublic
    @PostMapping("/{token}/order")
    @Operation(summary = "Receive CJ order status change webhooks (ORDER / ORDERSPLIT)")
    public ResponseEntity<Map<String, Object>> receiveOrderWebhook(@PathVariable String token,
            @RequestBody CjWebhookPayload<Map<String, Object>> rawPayload) {
        assertTokenMatches(token);

        String messageId = rawPayload.getMessageId();
        log.info("CJ order webhook received: messageId={} type={} messageType={}", messageId, rawPayload.getType(),
                rawPayload.getMessageType());

        if (isDuplicate(messageId)) {
            log.info("Duplicate webhook ignored: messageId={}", messageId);
            return ResponseEntity.ok(OK);
        }

        String rawJson = toJson(rawPayload);
        saveLog(messageId, rawPayload.getType(), rawPayload.getMessageType(), rawJson);

        try {
            String messageType = rawPayload.getMessageType();
            if (messageType != null && messageType.toUpperCase().contains("SPLIT")) {
                CjWebhookPayload<CjOrderSplitWebhookParams> typed = retype(rawJson, CjOrderSplitWebhookParams.class);
                orderWebhookHandler.handleOrderSplitEvent(typed.getParams());
            } else {
                CjWebhookPayload<CjOrderWebhookParams> typed = retype(rawJson, CjOrderWebhookParams.class);
                orderWebhookHandler.handleOrderEvent(messageType, typed.getParams());
            }
        } catch (Exception e) {
            log.error("Error processing order webhook messageId={}: {}", messageId, e.getMessage(), e);
        }

        return ResponseEntity.ok(OK);
    }

    // ─── LOGISTICS ───────────────────────────────────────────────────────────

    @NxPublic
    @PostMapping("/{token}/logistics")
    @Operation(summary = "Receive CJ logistics / tracking update webhooks")
    public ResponseEntity<Map<String, Object>> receiveLogisticsWebhook(@PathVariable String token,
            @RequestBody CjWebhookPayload<Map<String, Object>> rawPayload) {
        assertTokenMatches(token);

        String messageId = rawPayload.getMessageId();
        log.info("CJ logistics webhook received: messageId={} type={} messageType={}", messageId, rawPayload.getType(),
                rawPayload.getMessageType());

        if (isDuplicate(messageId)) {
            log.info("Duplicate webhook ignored: messageId={}", messageId);
            return ResponseEntity.ok(OK);
        }

        String rawJson = toJson(rawPayload);
        saveLog(messageId, rawPayload.getType(), rawPayload.getMessageType(), rawJson);

        try {
            CjWebhookPayload<CjLogisticWebhookParams> typed = retype(rawJson, CjLogisticWebhookParams.class);
            logisticWebhookHandler.handleLogisticEvent(typed.getParams());
        } catch (Exception e) {
            log.error("Error processing logistics webhook messageId={}: {}", messageId, e.getMessage(), e);
        }

        return ResponseEntity.ok(OK);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Verifies the path token matches the configured secret using a constant-time
     * comparison to avoid leaking timing-side-channel info. On mismatch (or when no
     * secret is configured), throws {@link EntityNotFoundException} so the response
     * is indistinguishable from any other 404 on the service.
     */
    private void assertTokenMatches(String token) {
        String expected = webhookProperties.getSecret();
        if (expected == null || expected.isBlank() || token == null
                || !MessageDigest.isEqual(token.getBytes(), expected.getBytes())) {
            log.warn("CJ webhook rejected: token mismatch");
            throw new EntityNotFoundException("NF-WEBHOOK", List.of("Not Found"));
        }
    }

    private boolean isDuplicate(String messageId) {
        return messageId != null && webhookLogRepository.existsByMessageId(messageId);
    }

    private void saveLog(String messageId, String type, String messageType, String rawJson) {
        if (messageId == null)
            return;
        CjWebhookLogEntity log = CjWebhookLogEntity.builder().messageId(messageId).type(type).messageType(messageType)
                .rawPayload(rawJson).processedAt(Instant.now()).build();
        webhookLogRepository.save(log);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise webhook payload: {}", e.getMessage());
            return "{}";
        }
    }

    private <T> CjWebhookPayload<T> retype(String json, Class<T> paramsClass) throws JsonProcessingException {
        var type = objectMapper.getTypeFactory().constructParametricType(CjWebhookPayload.class, paramsClass);
        return objectMapper.readValue(json, type);
    }
}
