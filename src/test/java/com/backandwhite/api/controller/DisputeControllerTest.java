package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.CjDisputeService;
import com.backandwhite.infrastructure.db.postgres.entity.CjDisputeEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DisputeControllerTest {

    @Mock
    private CjDisputeService disputeService;

    @InjectMocks
    private DisputeController controller;

    @Test
    void open_withJwt_returnsCreated() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("u1").claim("sub", "u1").build();
        CjDisputeEntity saved = CjDisputeEntity.builder().id("d1").build();
        when(disputeService.openDispute(eq("o1"), eq("CJ-1"), eq("damaged"), eq("desc"), anyList(), eq("u1")))
                .thenReturn(saved);
        DisputeController.OpenDisputeRequest req = new DisputeController.OpenDisputeRequest("CJ-1", "damaged", "desc",
                List.of("url"));
        var resp = controller.open("o1", req, jwt);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody()).isSameAs(saved);
    }

    @Test
    void open_nullJwt_passesNullActor() {
        when(disputeService.openDispute(eq("o1"), any(), any(), any(), any(), eq(null)))
                .thenReturn(CjDisputeEntity.builder().build());
        DisputeController.OpenDisputeRequest req = new DisputeController.OpenDisputeRequest("CJ-1", "damaged", "desc",
                List.of());
        var resp = controller.open("o1", req, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void listMine_returnsOk() {
        when(disputeService.listByOrder("o1")).thenReturn(List.of());
        var resp = controller.listMine("o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void adminList_returnsOk() {
        when(disputeService.listByStatus("OPEN")).thenReturn(List.of());
        var resp = controller.adminList("OPEN");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void transition_withJwt_returnsOk() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("admin").claim("sub", "admin").build();
        CjDisputeEntity saved = CjDisputeEntity.builder().id("d1").build();
        when(disputeService.transitionStatus(eq("d1"), eq("APPROVED"), eq("ok"), any(BigDecimal.class), eq("USD"),
                eq("admin"))).thenReturn(saved);
        DisputeController.TransitionRequest req = new DisputeController.TransitionRequest("APPROVED", "ok",
                new BigDecimal("10.00"), "USD");
        var resp = controller.transition("d1", req, jwt);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void transition_nullJwt_passesNullActor() {
        when(disputeService.transitionStatus(eq("d1"), any(), any(), any(), any(), eq(null)))
                .thenReturn(CjDisputeEntity.builder().build());
        DisputeController.TransitionRequest req = new DisputeController.TransitionRequest("APPROVED", null, null, null);
        var resp = controller.transition("d1", req, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
