package com.backandwhite.api.controller;

import com.backandwhite.api.dto.webhook.*;
import com.backandwhite.application.service.CjLogisticWebhookHandler;
import com.backandwhite.application.service.CjOrderWebhookHandler;
import com.backandwhite.infrastructure.db.postgres.entity.CjWebhookLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjWebhookLogJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Receives push notifications (webhooks) sent by CJ Dropshipping.
 * <p>
 * All endpoints:
 * <ul>
 * <li>Are publicly accessible (no auth header required — CJ does not send
 * one).</li>
 * <li>Perform idempotency checks via {@code cj_webhook_log}.</li>
 * <li>Must respond quickly with {@code {"result": true}} so CJ does not
 * retry.</li>
 * <li>Delegate actual processing to handler beans.</li>
 * </ul>
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

    /** CJ-required success response. */
    private static final Map<String, Object> OK = Map.of("result", true);

    // ─── ORDER + ORDERSPLIT ───────────────────────────────────────────────────

    @PostMapping("/order")
    @Operation(summary = "Receive CJ order status change webhooks (ORDER / ORDERSPLIT)")
    public ResponseEntity<Map<String, Object>> receiveOrderWebhook(
            @RequestBody CjWebhookPayload<Map<String, Object>> rawPayload) {

        String messageId = rawPayload.getMessageId();
        log.info("CJ order webhook received: messageId={} type={} messageType={}",
                messageId, rawPayload.getType(), rawPayload.getMessageType());

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

    @PostMapping("/logistics")
    @Operation(summary = "Receive CJ logistics / tracking update webhooks")
    public ResponseEntity<Map<String, Object>> receiveLogisticsWebhook(
            @RequestBody CjWebhookPayload<Map<String, Object>> rawPayload) {

        String messageId = rawPayload.getMessageId();
        log.info("CJ logistics webhook received: messageId={} type={} messageType={}",
                messageId, rawPayload.getType(), rawPayload.getMessageType());

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

    private boolean isDuplicate(String messageId) {
        return messageId != null && webhookLogRepository.existsByMessageId(messageId);
    }

    private void saveLog(String messageId, String type, String messageType, String rawJson) {
        if (messageId == null)
            return;
        CjWebhookLogEntity log = CjWebhookLogEntity.builder()
                .messageId(messageId)
                .type(type)
                .messageType(messageType)
                .rawPayload(rawJson)
                .processedAt(Instant.now())
                .build();
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
        var type = objectMapper.getTypeFactory()
                .constructParametricType(CjWebhookPayload.class, paramsClass);
        return objectMapper.readValue(json, type);
    }
}
