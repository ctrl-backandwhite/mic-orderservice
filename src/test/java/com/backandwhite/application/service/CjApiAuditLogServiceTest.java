package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.CjApiAuditLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjApiAuditLogJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjApiAuditLogService")
class CjApiAuditLogServiceTest {

    @Mock
    private CjApiAuditLogJpaRepository repository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CjApiAuditLogService service;

    @BeforeEach
    void setUp() {
        when(repository.save(any(CjApiAuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("redacts known PII keys before persisting the request body")
    void redactsRequestBody() {
        Map<String, Object> body = Map.of("phone", "+34987654321", "shippingAddress", "Calle Mayor 1", "qty", 2);
        CjApiAuditLogEntity saved = service.record("/order/createOrder", "ord-1", 200, "200000", "req-1", 120, body,
                Map.of("ok", true), null, 0);
        assertThat(saved.getRequestBody()).contains("***").doesNotContain("Calle Mayor");
        assertThat(saved.getEndpoint()).isEqualTo("/order/createOrder");
        assertThat(saved.getRetryAttempt()).isEqualTo((short) 0);
    }

    @Test
    @DisplayName("redacts PII inside string-encoded JSON payloads")
    void redactsStringJson() {
        String json = "{\"email\":\"jorge@example.com\",\"keep\":\"yes\"}";
        CjApiAuditLogEntity saved = service.record("/x", "ord", 200, null, null, 0, json, null, null, 0);
        assertThat(saved.getRequestBody()).doesNotContain("jorge@example.com").contains("***");
    }

    @Test
    @DisplayName("masks short PII values entirely with three asterisks")
    void shortValuesMasked() {
        CjApiAuditLogEntity saved = service.record("/x", "ord", 200, null, null, 0, Map.of("phone", "12"), null, null,
                0);
        assertThat(saved.getRequestBody()).contains("***");
    }

    @Test
    @DisplayName("returns null when bodies are null")
    void nullBodies() {
        CjApiAuditLogEntity saved = service.record("/x", "ord", 200, null, null, 0, null, null, null, 0);
        assertThat(saved.getRequestBody()).isNull();
        assertThat(saved.getResponseBody()).isNull();
    }

    @Test
    @DisplayName("redaction tolerates malformed string payloads by returning {} fallback")
    void malformedJsonFallback() {
        CjApiAuditLogEntity saved = service.record("/x", "ord", 200, null, null, 0, "not-a-valid-json", null, null, 0);
        assertThat(saved.getRequestBody()).isEqualTo("{}");
    }

    @Test
    @DisplayName("findByOrder delegates to the repository")
    void findByOrderDelegates() {
        org.mockito.Mockito.reset(repository);
        when(repository.findAllByOrderIdOrderByCreatedAtAsc("ord-1")).thenReturn(List.of());
        assertThat(service.findByOrder("ord-1")).isEmpty();
    }

    @Test
    @DisplayName("nightly purge deletes rows older than 12 months")
    void purgeOldEntries() {
        org.mockito.Mockito.reset(repository);
        when(repository.deleteOlderThan(any())).thenReturn(3);
        service.purgeOldEntries();
        verify(repository).deleteOlderThan(any());
    }

    @Test
    @DisplayName("purge silently ignores when nothing was deleted")
    void purgeNothing() {
        org.mockito.Mockito.reset(repository);
        when(repository.deleteOlderThan(any())).thenReturn(0);
        service.purgeOldEntries();
        verify(repository).deleteOlderThan(any());
    }

    @Test
    @DisplayName("redacts arrays and nested objects recursively")
    void redactsNested() {
        Map<String, Object> body = Map.of("items", List.of(Map.of("recipientName", "Alice Smith")));
        ArgumentCaptor<CjApiAuditLogEntity> captor = ArgumentCaptor.forClass(CjApiAuditLogEntity.class);
        service.record("/x", "ord", 200, null, null, 0, body, null, null, 0);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRequestBody()).doesNotContain("Alice Smith");
    }

    @Test
    @DisplayName("truncates redacted body to MAX_BODY_BYTES (32K) instead of dumping huge JSON")
    void truncatesLargeBody() {
        // Build payload larger than the 32K cap to trip the substring path.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++)
            sb.append("AAAAAAAAAA"); // 40K chars
        Map<String, String> body = java.util.Collections.singletonMap("payload", sb.toString());
        ArgumentCaptor<CjApiAuditLogEntity> captor = ArgumentCaptor.forClass(CjApiAuditLogEntity.class);
        service.record("/x", "ord", 200, null, null, 0, body, null, null, 0);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRequestBody().length()).isLessThanOrEqualTo(32 * 1024);
    }

    @Test
    @DisplayName("a NullNode body inside the JSON tree is left alone (no redact crash)")
    void handlesNullNode() {
        com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
        node.putNull("phone");
        node.put("keep", "yes");
        ArgumentCaptor<CjApiAuditLogEntity> captor = ArgumentCaptor.forClass(CjApiAuditLogEntity.class);
        service.record("/x", "ord", 200, null, null, 0, node, null, null, 0);
        verify(repository).save(captor.capture());
        // null phone should remain null (not crash)
        assertThat(captor.getValue().getRequestBody()).contains("\"phone\":null");
    }

    @Test
    @DisplayName("mask helper preserves null and empty inputs as-is")
    void maskNullEmpty() throws Exception {
        org.mockito.Mockito.reset(repository);
        java.lang.reflect.Method m = CjApiAuditLogService.class.getDeclaredMethod("mask", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(null, (Object) null)).isNull();
        assertThat(m.invoke(null, "")).isEqualTo("");
    }
}
