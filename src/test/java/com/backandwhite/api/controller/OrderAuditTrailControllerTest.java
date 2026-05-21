package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.CjApiAuditLogService;
import com.backandwhite.application.service.OrderFinancialLedgerService;
import com.backandwhite.application.service.OrderItemSnapshotService;
import com.backandwhite.application.service.OrderStateHistoryService;
import com.backandwhite.infrastructure.db.postgres.entity.CjWebhookLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjWebhookLogJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderAuditTrailControllerTest {

    @Mock
    private OrderStateHistoryService stateHistoryService;
    @Mock
    private CjApiAuditLogService auditLogService;
    @Mock
    private OrderFinancialLedgerService ledgerService;
    @Mock
    private OrderItemSnapshotService snapshotService;
    @Mock
    private CjWebhookLogJpaRepository webhookLogRepository;

    @InjectMocks
    private OrderAuditTrailController controller;

    @Test
    void auditTrail_returnsConsolidatedPayload() {
        CjWebhookLogEntity matching = CjWebhookLogEntity.builder().messageId("m1").rawPayload("payload-with-o1")
                .build();
        CjWebhookLogEntity nonMatching = CjWebhookLogEntity.builder().messageId("m2").rawPayload("other").build();
        CjWebhookLogEntity nullPayload = CjWebhookLogEntity.builder().messageId("m3").rawPayload(null).build();
        when(webhookLogRepository.findAll()).thenReturn(List.of(matching, nonMatching, nullPayload));
        when(stateHistoryService.findOrderHistory("o1")).thenReturn(List.of());
        when(stateHistoryService.findCjHistoryByOrder("o1")).thenReturn(List.of());
        when(auditLogService.findByOrder("o1")).thenReturn(List.of());
        when(ledgerService.findByOrder("o1")).thenReturn(List.of());
        when(snapshotService.findByOrder("o1")).thenReturn(List.of());

        var resp = controller.auditTrail("o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsKey("orderId").containsKey("stateHistory").containsKey("cjStateHistory")
                .containsKey("cjApiCalls").containsKey("ledger").containsKey("webhooks").containsKey("snapshot");
        @SuppressWarnings("unchecked")
        List<CjWebhookLogEntity> webhooks = (List<CjWebhookLogEntity>) resp.getBody().get("webhooks");
        assertThat(webhooks).containsExactly(matching);
    }
}
