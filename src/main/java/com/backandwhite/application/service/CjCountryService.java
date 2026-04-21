package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.CjAllowedCountryEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjAllowedCountryJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 3 — canonical source of truth for the countries CJ Dropshipping is
 * willing to ship to. Checkout calls {@link #isAllowed(String)} before letting
 * a customer pay; admin UIs use the CRUD helpers.
 */
@Service
@RequiredArgsConstructor
public class CjCountryService {

    private final CjAllowedCountryJpaRepository repository;

    public boolean isAllowed(String rawCountryCode) {
        String code = normalise(rawCountryCode);
        if (code == null) {
            return false;
        }
        return repository.existsByCountryCodeAndActiveTrue(code);
    }

    public List<CjAllowedCountryEntity> listActive() {
        return repository.findAllByActiveTrueOrderByCountryCodeAsc();
    }

    public List<CjAllowedCountryEntity> listAll() {
        return repository.findAll();
    }

    @Transactional
    public CjAllowedCountryEntity upsert(String rawCode, String name, boolean active) {
        String code = normalise(rawCode);
        if (code == null) {
            throw new IllegalArgumentException("Invalid country code");
        }
        Instant now = Instant.now();
        CjAllowedCountryEntity entity = repository.findById(code)
                .orElseGet(() -> CjAllowedCountryEntity.builder().countryCode(code).createdAt(now).build());
        entity.setCountryName(name != null ? name.trim() : code);
        entity.setActive(active);
        entity.setUpdatedAt(now);
        return repository.save(entity);
    }

    @Transactional
    public void deactivate(String rawCode) {
        String code = normalise(rawCode);
        if (code == null) {
            return;
        }
        repository.findById(code).ifPresent(e -> {
            e.setActive(false);
            e.setUpdatedAt(Instant.now());
            repository.save(e);
        });
    }

    private static String normalise(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() == 2 ? trimmed : null;
    }
}
