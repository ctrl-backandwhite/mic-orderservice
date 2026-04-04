package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.repository.TaxRuleRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.TaxRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaxRuleRepositoryImpl implements TaxRuleRepository {

    private final TaxRuleJpaRepository jpa;
    private final ShippingTaxInfraMapper mapper;

    @Override
    public TaxRule save(TaxRule rule) {
        rule.setId(UUID.randomUUID().toString());
        return mapper.toTaxDomain(jpa.save(mapper.toTaxEntity(rule)));
    }

    @Override
    public TaxRule update(TaxRule rule) {
        return mapper.toTaxDomain(jpa.save(mapper.toTaxEntity(rule)));
    }

    @Override
    public Optional<TaxRule> findById(String id) {
        return jpa.findById(id).map(mapper::toTaxDomain);
    }

    @Override
    public Page<TaxRule> findAll(Map<String, Object> filters, Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toTaxDomain);
    }

    @Override
    public List<TaxRule> findByCountryAndRegion(String country, String region) {
        if (region != null && !region.isBlank()) {
            return jpa.findByCountryAndRegionAndActiveTrue(country, region).stream()
                    .map(mapper::toTaxDomain).toList();
        }
        return jpa.findByCountryAndActiveTrue(country).stream()
                .map(mapper::toTaxDomain).toList();
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }
}
