package com.backandwhite.api.controller;

import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.infrastructure.db.postgres.entity.AppSettingEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AppSettingJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 20 — kill switches and feature flags live in {@code app_settings} so an
 * operator can flip them without a deploy. The most important one today is
 * {@code CJ_ENABLED}: flipping it to {@code false} makes the checkout show the
 * "temporarily unavailable" banner and stops the fulfillment pipeline from
 * dispatching new orders to CJ.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/settings")
@Tag(name = "App Settings", description = "Runtime flags / kill switches stored in app_settings")
public class AppSettingsAdminController {

    private static final String KEY_VALUE = "value";

    private final AppSettingJpaRepository repository;

    @NxAdmin
    @GetMapping("/{key}")
    @Operation(summary = "Read a runtime setting by key")
    public ResponseEntity<Map<String, String>> get(@PathVariable String key) {
        return repository.findById(key).map(
                e -> ResponseEntity.ok(Map.of("key", e.getKey(), KEY_VALUE, e.getValue() != null ? e.getValue() : "")))
                .orElse(ResponseEntity.ok(Map.of("key", key, KEY_VALUE, "")));
    }

    @NxAdmin
    @PutMapping("/{key}/{value}")
    @Operation(summary = "Write a runtime setting (creates or updates)")
    public ResponseEntity<Map<String, String>> put(@PathVariable String key, @PathVariable String value) {
        AppSettingEntity entity = repository.findById(key).orElseGet(() -> AppSettingEntity.builder().key(key).build());
        entity.setValue(value);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        return ResponseEntity.ok(Map.of("key", key, KEY_VALUE, value));
    }
}
