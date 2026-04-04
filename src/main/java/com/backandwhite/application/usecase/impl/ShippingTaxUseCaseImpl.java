package com.backandwhite.application.usecase.impl;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.repository.ShippingCarrierRepository;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.domain.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;

@Log4j2
@Service
@RequiredArgsConstructor
public class ShippingTaxUseCaseImpl implements ShippingTaxUseCase {

    private final ShippingCarrierRepository carrierRepository;
    private final ShippingRuleRepository ruleRepository;
    private final TaxRuleRepository taxRuleRepository;

    // ---- Carriers ----
    @Override
    @Transactional
    public ShippingCarrier createCarrier(ShippingCarrier carrier) {
        return carrierRepository.save(carrier);
    }

    @Override
    @Transactional
    public ShippingCarrier updateCarrier(String id, ShippingCarrier carrier) {
        carrierRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingCarrier", id));
        carrier.setId(id);
        return carrierRepository.update(carrier);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingCarrier findCarrierById(String id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingCarrier", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<ShippingCarrier> findAllCarriers(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(carrierRepository.findAll(Map.of(), pageable));
    }

    @Override
    @Transactional
    public void deleteCarrier(String id) {
        carrierRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingCarrier", id));
        carrierRepository.delete(id);
    }

    // ---- Rules ----
    @Override
    @Transactional
    public ShippingRule createRule(ShippingRule rule) {
        return ruleRepository.save(rule);
    }

    @Override
    @Transactional
    public ShippingRule updateRule(String id, ShippingRule rule) {
        ruleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingRule", id));
        rule.setId(id);
        return ruleRepository.update(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingRule findRuleById(String id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingRule", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<ShippingRule> findAllRules(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(ruleRepository.findAll(Map.of(), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingRule> findShippingOptions(String country, BigDecimal weight, BigDecimal subtotal) {
        return ruleRepository.findOptions(country, weight, subtotal);
    }

    @Override
    @Transactional
    public void deleteRule(String id) {
        ruleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("ShippingRule", id));
        ruleRepository.delete(id);
    }

    // ---- Tax ----
    @Override
    @Transactional
    public TaxRule createTaxRule(TaxRule rule) {
        return taxRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public TaxRule updateTaxRule(String id, TaxRule rule) {
        taxRuleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("TaxRule", id));
        rule.setId(id);
        return taxRuleRepository.update(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxRule findTaxRuleById(String id) {
        return taxRuleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("TaxRule", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<TaxRule> findAllTaxRules(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(taxRuleRepository.findAll(Map.of(), pageable));
    }

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.10");

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTax(String country, String region, BigDecimal subtotal) {
        // 1. Try exact country + region
        List<TaxRule> rules = taxRuleRepository.findByCountryAndRegion(country, region);

        // 2. Fallback: country only (blank region)
        if (rules.isEmpty() && region != null && !region.isBlank()) {
            rules = taxRuleRepository.findByCountryAndRegion(country, null);
        }

        // 3. Default 10 % when no rules are configured
        BigDecimal rate = rules.isEmpty()
                ? DEFAULT_TAX_RATE
                : rules.stream()
                        .map(TaxRule::getRate)
                        .max(BigDecimal::compareTo)
                        .orElse(DEFAULT_TAX_RATE);

        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void deleteTaxRule(String id) {
        taxRuleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("TaxRule", id));
        taxRuleRepository.delete(id);
    }
}
