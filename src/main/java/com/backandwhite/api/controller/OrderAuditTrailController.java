package com.backandwhite.api.controller;

import com.backandwhite.application.service.CjApiAuditLogService;
import com.backandwhite.application.service.OrderFinancialLedgerService;
import com.backandwhite.application.service.OrderItemSnapshotService;
import com.backandwhite.application.service.OrderStateHistoryService;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.infrastructure.db.postgres.repository.CjWebhookLogJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 8.4 — single endpoint that returns every piece of history we have about
 * one order so support / accounting can reconstruct what happened without
 * running SQL against prod.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Order Audit Trail", description = "Consolidated timeline view of an order")
public class OrderAuditTrailController {

    private final OrderStateHistoryService stateHistoryService;
    private final CjApiAuditLogService auditLogService;
    private final OrderFinancialLedgerService ledgerService;
    private final OrderItemSnapshotService snapshotService;
    private final CjWebhookLogJpaRepository webhookLogRepository;

    @NxAdmin
    @GetMapping("/{orderId}/audit-trail")
    @Operation(summary = "Consolidated audit trail — state history + CJ API calls + ledger + webhooks + snapshot")
    public ResponseEntity<Map<String, Object>> auditTrail(@PathVariable String orderId) {
        Map<String, Object> payload = Map.of("orderId", orderId, "stateHistory",
                stateHistoryService.findOrderHistory(orderId), "cjStateHistory",
                stateHistoryService.findCjHistoryByOrder(orderId), "cjApiCalls", auditLogService.findByOrder(orderId),
                "ledger", ledgerService.findByOrder(orderId), "webhooks",
                webhookLogRepository.findAll().stream()
                        .filter(w -> w.getRawPayload() != null && w.getRawPayload().contains(orderId)).toList(),
                "snapshot", snapshotService.findByOrder(orderId));
        return ResponseEntity.ok(payload);
    }
}
