package com.backandwhite.api.controller;

import com.backandwhite.application.service.CjDisputeService;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.infrastructure.db.postgres.entity.CjDisputeEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 13 — customer-facing (open) and admin-facing (list + transition)
 * endpoints for the dispute / returns / refunds flow.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Disputes", description = "Customer and admin dispute management")
public class DisputeController {

    private final CjDisputeService disputeService;

    @NxUser
    @PostMapping("/api/v1/orders/{orderId}/dispute")
    @Operation(summary = "Customer opens a dispute on a delivered order")
    public ResponseEntity<CjDisputeEntity> open(@PathVariable String orderId, @RequestBody OpenDisputeRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        String actorId = jwt != null ? jwt.getSubject() : null;
        CjDisputeEntity saved = disputeService.openDispute(orderId, req.cjOrderId(), req.reason(), req.description(),
                req.evidenceUrls(), actorId);
        return ResponseEntity.status(201).body(saved);
    }

    @NxUser
    @GetMapping("/api/v1/orders/{orderId}/disputes")
    @Operation(summary = "List disputes for one of my orders")
    public ResponseEntity<List<CjDisputeEntity>> listMine(@PathVariable String orderId) {
        return ResponseEntity.ok(disputeService.listByOrder(orderId));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @NxAdmin
    @GetMapping("/api/v1/admin/disputes")
    @Operation(summary = "Admin list disputes filtered by status")
    public ResponseEntity<List<CjDisputeEntity>> adminList(@RequestParam(defaultValue = "OPEN") String status) {
        return ResponseEntity.ok(disputeService.listByStatus(status));
    }

    @NxAdmin
    @PatchMapping("/api/v1/admin/disputes/{disputeId}")
    @Operation(summary = "Admin transitions a dispute to a new status")
    public ResponseEntity<CjDisputeEntity> transition(@PathVariable String disputeId,
            @RequestBody TransitionRequest req, @AuthenticationPrincipal Jwt jwt) {
        String actorId = jwt != null ? jwt.getSubject() : null;
        CjDisputeEntity saved = disputeService.transitionStatus(disputeId, req.status(), req.resolution(),
                req.refundAmount(), req.refundCurrency(), actorId);
        return ResponseEntity.ok(saved);
    }

    public record OpenDisputeRequest(String cjOrderId, String reason, String description, List<String> evidenceUrls) {
    }

    public record TransitionRequest(String status, String resolution, BigDecimal refundAmount, String refundCurrency) {
    }
}
