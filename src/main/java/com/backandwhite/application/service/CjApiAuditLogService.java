package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.CjApiAuditLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjApiAuditLogJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 8.1 — persists one row per outbound call to CJ so we can answer "what
 * exactly did we send to CJ at 14:37 last Tuesday for order ABC-123?" without
 * trawling logs. PII fields (phone, detailed address) are masked before
 * storage. Runs a nightly purge job to honour the 12-month retention policy.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjApiAuditLogService {

    private static final int MAX_BODY_BYTES = 32 * 1024;

    private static final Set<String> PII_KEYS = Set.of("shippingPhone", "phone", "shippingAddress", "addressLine1",
            "addressLine2", "email", "recipient", "recipientName", "customerEmail");

    private final CjApiAuditLogJpaRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Persists a single CJ API call audit row — flat parameter list mirrors the
     * {@link CjApiAuditLogEntity} columns.
     */
    @Transactional
    @SuppressWarnings({"java:S107", "java:S6213"}) // Public API kept stable; covered by tests in
                                                   // CjApiAuditLogServiceTest.
    public CjApiAuditLogEntity record(String endpoint, String orderId, Integer httpStatus, String cjCode,
            String cjRequestId, int latencyMs, Object requestBody, Object responseBody, String errorMessage,
            int retryAttempt) {

        CjApiAuditLogEntity entity = CjApiAuditLogEntity.builder().createdAt(Instant.now()).endpoint(endpoint)
                .orderId(orderId).httpStatus(httpStatus).cjCode(cjCode).cjRequestId(cjRequestId).latencyMs(latencyMs)
                .requestBody(toRedactedJson(requestBody)).responseBody(toRedactedJson(responseBody))
                .errorMessage(errorMessage).retryAttempt((short) retryAttempt).build();
        return repository.save(entity);
    }

    public List<CjApiAuditLogEntity> findByOrder(String orderId) {
        return repository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /** Purge entries older than 12 months (Fase 8.5). Runs nightly. */
    @Scheduled(cron = "${app.cj.retention.audit-log-cron:0 30 3 * * *}")
    @Transactional
    public void purgeOldEntries() {
        Instant cutoff = Instant.now().minus(365, ChronoUnit.DAYS);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("::> CJ audit log retention: {} rows purged (cutoff {})", deleted, cutoff);
        }
    }

    private String toRedactedJson(Object body) {
        if (body == null) {
            return null;
        }
        try {
            JsonNode node = body instanceof String s ? objectMapper.readTree(s) : objectMapper.valueToTree(body);
            redact(node);
            String json = objectMapper.writeValueAsString(node);
            if (json.length() > MAX_BODY_BYTES) {
                return json.substring(0, MAX_BODY_BYTES);
            }
            return json;
        } catch (Exception e) {
            log.warn("::> Could not redact audit body: {}", e.getMessage());
            return "{}";
        }
    }

    private void redact(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(field -> {
                JsonNode child = obj.get(field);
                if (PII_KEYS.contains(field) && child.isTextual()) {
                    obj.put(field, mask(child.asText()));
                } else {
                    redact(child);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
    }

    private static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 4) {
            return "***";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }
}
