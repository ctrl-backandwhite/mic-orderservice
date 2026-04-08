package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.repository.ShippingCarrierRepository;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.domain.repository.TaxRuleRepository;
import com.backandwhite.common.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    public PageResult<ShippingCarrier> findAllCarriers(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(carrierRepository.findAll(Map.of(), pageable));
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
    public PageResult<ShippingRule> findAllRules(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(ruleRepository.findAll(Map.of(), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingRule> findShippingOptions(String country, BigDecimal weight, Money subtotal) {
        List<String> zones = resolveZones(country);
        return zones.stream()
                .flatMap(zone -> ruleRepository.findOptions(zone, weight, subtotal).stream())
                .toList();
    }

    /**
     * Map ISO 3166-1 alpha-2 country code to applicable shipping zone names.
     * For Spain, returns all sub-zones so the user can choose.
     */
    private List<String> resolveZones(String countryCode) {
        if (countryCode == null) return List.of("Resto del mundo");
        return switch (countryCode.toUpperCase()) {
            case "ES" -> List.of("España Peninsular", "Baleares", "Canarias");
            case "US" -> List.of("Estados Unidos");
            case "PT" -> List.of("Portugal");
            case "GB" -> List.of("Reino Unido");
            case "FR", "DE", "IT", "NL", "BE", "AT", "IE", "FI", "SE", "DK",
                 "PL", "CZ", "SK", "HU", "RO", "BG", "HR", "SI", "LT", "LV",
                 "EE", "CY", "MT", "LU", "GR" -> List.of("Unión Europea");
            default -> List.of("Resto del mundo");
        };
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
    public PageResult<TaxRule> findAllTaxRules(int page, int size, String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(taxRuleRepository.findAll(Map.of(), pageable));
    }

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.10");

    @Override
    @Transactional(readOnly = true)
    public Money calculateTax(String country, String region, Money subtotal) {
        // 1. Try exact country + region
        List<TaxRule> rules = taxRuleRepository.findByCountryAndRegion(country, region);

        // 2. Fallback: country only (blank region)
        if (rules.isEmpty() && region != null && !region.isBlank()) {
            rules = taxRuleRepository.findByCountryAndRegion(country, null);
        }

        // 3. Default 10 % when no rules are configured
        if (rules.isEmpty()) {
            return subtotal.multiply(DEFAULT_TAX_RATE);
        }

        // Pick the rule with the highest rate
        TaxRule bestRule = rules.stream()
                .max(java.util.Comparator.comparing(TaxRule::getRate))
                .orElse(null);

        if (bestRule == null) {
            return subtotal.multiply(DEFAULT_TAX_RATE);
        }

        // 4. Respect TaxType: FIXED uses rate as flat amount, everything else as
        // percentage
        if (bestRule.getType() == com.backandwhite.domain.valueobject.TaxType.FIXED) {
            return Money.of(bestRule.getRate());
        }

        // PERCENTAGE, VAT, SALES, GST — all treated as percentage of subtotal
        return subtotal.multiply(bestRule.getRate());
    }

    @Override
    @Transactional
    public void deleteTaxRule(String id) {
        taxRuleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("TaxRule", id));
        taxRuleRepository.delete(id);
    }
}
