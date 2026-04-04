package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ShippingRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShippingRuleRepositoryImpl implements ShippingRuleRepository {

    private final ShippingRuleJpaRepository jpa;
    private final ShippingTaxInfraMapper mapper;

    @Override
    public ShippingRule save(ShippingRule rule) {
        rule.setId(UUID.randomUUID().toString());
        return mapper.toRuleDomain(jpa.save(mapper.toRuleEntity(rule)));
    }

    @Override
    public ShippingRule update(ShippingRule rule) {
        return mapper.toRuleDomain(jpa.save(mapper.toRuleEntity(rule)));
    }

    @Override
    public Optional<ShippingRule> findById(String id) {
        return jpa.findById(id).map(mapper::toRuleDomain);
    }

    @Override
    public Page<ShippingRule> findAll(Map<String, Object> filters, Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toRuleDomain);
    }

    @Override
    public List<ShippingRule> findOptions(String country, BigDecimal weight, BigDecimal subtotal) {
        return jpa.findApplicableRules(country, weight, subtotal)
                .stream()
                .map(entity -> {
                    ShippingRule rule = mapper.toRuleDomain(entity);
                    if (rule.getFreeAbove() != null && subtotal.compareTo(rule.getFreeAbove()) >= 0) {
                        rule.setRate(BigDecimal.ZERO);
                    }
                    return rule;
                })
                .toList();
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }
}
