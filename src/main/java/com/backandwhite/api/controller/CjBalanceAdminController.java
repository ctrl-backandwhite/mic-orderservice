package com.backandwhite.api.controller;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.common.security.annotation.NxAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 14 — admin dashboard endpoint for CJ balance. Returns current balance,
 * configured threshold, and headroom so the admin UI can render an at-a-glance
 * status widget.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/cj/balance")
@Tag(name = "CJ Balance", description = "CJ Dropshipping account balance monitoring")
public class CjBalanceAdminController {

    private final CjShoppingPort cjShoppingPort;

    @Value("${app.cj.min-balance-alert:50.00}")
    private BigDecimal minBalanceAlert;

    @NxAdmin
    @GetMapping
    @Operation(summary = "Current CJ balance and threshold snapshot")
    public ResponseEntity<Map<String, Object>> current() {
        BigDecimal balance = cjShoppingPort.getBalanceAmount();
        boolean belowThreshold = balance != null && balance.compareTo(minBalanceAlert) < 0;
        return ResponseEntity.ok(Map.of("balanceUsd", balance != null ? balance : BigDecimal.ZERO, "thresholdUsd",
                minBalanceAlert, "belowThreshold", belowThreshold));
    }
}
