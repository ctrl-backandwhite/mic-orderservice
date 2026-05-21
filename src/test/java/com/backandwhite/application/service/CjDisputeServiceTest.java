package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.CjDisputeEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjDisputeJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjDisputeService")
class CjDisputeServiceTest {

    @Mock
    private CjDisputeJpaRepository repository;

    @Mock
    private OrderStateHistoryService stateHistoryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CjDisputeService service;

    @Test
    @DisplayName("openDispute persists OPEN status and writes a state-history transition")
    void openDispute() throws JsonProcessingException {
        when(repository.save(any(CjDisputeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"u\"]");
        CjDisputeEntity saved = service.openDispute("ord-1", "cj-1", "DAMAGED", "broken", List.of("u"), "actor-1");
        assertThat(saved.getStatus()).isEqualTo(CjDisputeService.STATUS_OPEN);
        assertThat(saved.getEvidenceUrls()).isEqualTo("[\"u\"]");
        verify(stateHistoryService).recordOrderTransition(eq("ord-1"), eq(null), eq("DISPUTE_OPEN"),
                eq(OrderStateHistoryService.ACTOR_CUSTOMER), eq("actor-1"), any(), eq(null));
    }

    @Test
    @DisplayName("openDispute serialises null evidence list as null payload")
    void openDisputeNullEvidence() {
        when(repository.save(any(CjDisputeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        CjDisputeEntity saved = service.openDispute("ord-1", "cj-1", "WRONG", "no list", null, "actor-1");
        assertThat(saved.getEvidenceUrls()).isNull();
    }

    @Test
    @DisplayName("openDispute swallows JSON serialisation failures and stores []")
    void openDisputeSerialisationFailure() throws JsonProcessingException {
        when(repository.save(any(CjDisputeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });
        CjDisputeEntity saved = service.openDispute("ord-1", "cj-1", "X", "d", List.of("u"), "actor-1");
        assertThat(saved.getEvidenceUrls()).isEqualTo("[]");
    }

    @Test
    @DisplayName("transitionStatus updates fields and records history")
    void transitionStatus() {
        CjDisputeEntity dispute = CjDisputeEntity.builder().id("d-1").orderId("ord-1").status("OPEN").build();
        when(repository.findById("d-1")).thenReturn(Optional.of(dispute));
        when(repository.save(any(CjDisputeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        CjDisputeEntity result = service.transitionStatus("d-1", "APPROVED", "ok", new BigDecimal("12.50"), "USD",
                "admin-1");
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getResolution()).isEqualTo("ok");
        assertThat(result.getRefundAmount()).isEqualByComparingTo("12.50");
        assertThat(result.getRefundCurrency()).isEqualTo("USD");
        verify(stateHistoryService).recordOrderTransition(eq("ord-1"), eq("DISPUTE_OPEN"), eq("DISPUTE_APPROVED"),
                eq(OrderStateHistoryService.ACTOR_ADMIN), eq("admin-1"), any(), eq(null));
    }

    @Test
    @DisplayName("transitionStatus throws when the dispute does not exist")
    void transitionMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transitionStatus("missing", "APPROVED", null, null, null, "x"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Dispute not found");
    }

    @Test
    @DisplayName("transitionStatus skips optional fields when null")
    void transitionStatusOptionalNulls() {
        CjDisputeEntity dispute = CjDisputeEntity.builder().id("d").orderId("ord").status("OPEN").resolution("prev")
                .build();
        when(repository.findById("d")).thenReturn(Optional.of(dispute));
        when(repository.save(any(CjDisputeEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        CjDisputeEntity result = service.transitionStatus("d", "REJECTED", null, null, null, "admin");
        assertThat(result.getResolution()).isEqualTo("prev");
        assertThat(result.getRefundAmount()).isNull();
    }

    @Test
    @DisplayName("listByOrder delegates to the repository")
    void listByOrder() {
        when(repository.findAllByOrderIdOrderByCreatedAtDesc("ord")).thenReturn(List.of());
        assertThat(service.listByOrder("ord")).isEmpty();
    }

    @Test
    @DisplayName("listByStatus delegates to the repository")
    void listByStatus() {
        when(repository.findAllByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(List.of());
        assertThat(service.listByStatus("OPEN")).isEmpty();
    }
}
