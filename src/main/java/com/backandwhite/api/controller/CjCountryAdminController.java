package com.backandwhite.api.controller;

import com.backandwhite.application.service.CjCountryService;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.infrastructure.db.postgres.entity.CjAllowedCountryEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 3 admin endpoints to manage the whitelist of destination countries that
 * CJ Dropshipping is configured to ship to.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/cj/countries")
@Tag(name = "CJ Allowed Countries", description = "Admin management of CJ-shippable destination countries")
public class CjCountryAdminController {

    private final CjCountryService countryService;

    @NxAdmin
    @GetMapping
    @Operation(summary = "List all allowed countries")
    public ResponseEntity<List<CjAllowedCountryEntity>> list() {
        return ResponseEntity.ok(countryService.listAll());
    }

    @NxAdmin
    @PutMapping("/{countryCode}")
    @Operation(summary = "Create or update a country entry")
    public ResponseEntity<CjAllowedCountryEntity> upsert(@PathVariable String countryCode,
            @RequestBody UpsertBody body) {
        var saved = countryService.upsert(countryCode, body.name(), body.active());
        return ResponseEntity.ok(saved);
    }

    @NxAdmin
    @DeleteMapping("/{countryCode}")
    @Operation(summary = "Deactivate a country (keeps historical references)")
    public ResponseEntity<Void> deactivate(@PathVariable String countryCode) {
        countryService.deactivate(countryCode);
        return ResponseEntity.noContent().build();
    }

    public record UpsertBody(String name, boolean active) {
    }
}
