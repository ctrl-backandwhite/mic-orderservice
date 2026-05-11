package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ShippingRuleJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
    public List<ShippingRule> findOptions(String country, BigDecimal weight, Money subtotal) {
        return jpa.findApplicableRules(country, weight, subtotal.getAmount()).stream().map(entity -> {
            ShippingRule rule = mapper.toRuleDomain(entity);
            // Free-shipping promo applies when the order subtotal hits the
            // threshold AND the shipment isn't above the configured weight
            // cap. The weight cap (free_above_max_weight) is per-rule so
            // bulky-package carriers can opt out of granting the discount.
            // A null cap means "no weight limit" — promo applies on subtotal
            // alone.
            boolean meetsSubtotal = rule.getFreeAbove() != null && subtotal.isGreaterThanOrEqual(rule.getFreeAbove());
            boolean withinWeightCap = rule.getFreeAboveMaxWeight() == null
                    || weight.compareTo(rule.getFreeAboveMaxWeight()) <= 0;
            if (meetsSubtotal && withinWeightCap) {
                rule.setRate(Money.zero());
            }
            return rule;
        }).toList();
    }

    @Override
    public void delete(String id) {
        jpa.deleteById(id);
    }
}
