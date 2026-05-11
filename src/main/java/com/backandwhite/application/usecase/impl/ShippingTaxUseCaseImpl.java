package com.backandwhite.application.usecase.impl;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;

import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.repository.ShippingCarrierRepository;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.domain.repository.TaxRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ShippingTaxUseCaseImpl implements ShippingTaxUseCase {

    private static final String ENTITY_SHIPPING_CARRIER = "ShippingCarrier";
    private static final String ENTITY_SHIPPING_RULE = "ShippingRule";
    private static final String ENTITY_TAX_RULE = "TaxRule";

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
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_CARRIER, id));
        carrier.setId(id);
        return carrierRepository.update(carrier);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingCarrier findCarrierById(String id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_CARRIER, id));
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
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_CARRIER, id));
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
        ruleRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_RULE, id));
        rule.setId(id);
        return ruleRepository.update(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingRule findRuleById(String id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_RULE, id));
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
        log.info("::> findShippingOptions country='{}' resolved zones={} weight={} subtotal={}", country, zones, weight,
                subtotal.getAmount());
        List<ShippingRule> result = zones.stream()
                .flatMap(zone -> ruleRepository.findOptions(zone, weight, subtotal).stream()).toList();
        log.info("::> findShippingOptions returning {} options", result.size());
        return result;
    }

    /**
     * Resolve country code to zone identifier used in shipping_rules.zone. Zones
     * store the ISO 3166-1 alpha-2 code, but callers (e.g. orders) sometimes pass
     * the human display name ("España", "United States"). This helper accepts both:
     * 2-letter input is taken as-is, anything else is matched against display names
     * in es/en/pt locales so a Spanish-speaking user typing "España" still hits the
     * ES rules.
     */
    private List<String> resolveZones(String countryCode) {
        if (countryCode == null || countryCode.isBlank())
            return List.of();
        String trimmed = countryCode.trim();
        if (trimmed.length() == 2) {
            return List.of(trimmed.toUpperCase());
        }
        String iso = nameToIso(trimmed);
        return iso != null ? List.of(iso) : List.of(trimmed.toUpperCase());
    }

    private String nameToIso(String displayName) {
        for (String iso : java.util.Locale.getISOCountries()) {
            java.util.Locale loc = java.util.Locale.of("", iso);
            if (loc.getDisplayCountry(java.util.Locale.ENGLISH).equalsIgnoreCase(displayName)
                    || loc.getDisplayCountry(java.util.Locale.of("es")).equalsIgnoreCase(displayName)
                    || loc.getDisplayCountry(java.util.Locale.of("pt")).equalsIgnoreCase(displayName)) {
                return iso;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteRule(String id) {
        ruleRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_SHIPPING_RULE, id));
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
        taxRuleRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_TAX_RULE, id));
        rule.setId(id);
        return taxRuleRepository.update(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxRule findTaxRuleById(String id) {
        return taxRuleRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_TAX_RULE, id));
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
        // Normalise display names ("España", "Spain") to ISO so admins can
        // configure tax_rules.country with the canonical 2-letter code and
        // still hit them when the order's address has the localized name.
        String normalisedCountry = country;
        if (country != null && country.length() != 2) {
            String iso = nameToIso(country.trim());
            if (iso != null)
                normalisedCountry = iso;
        }

        // 1. Try exact country + region
        List<TaxRule> rules = taxRuleRepository.findByCountryAndRegion(normalisedCountry, region);

        // 2. Fallback: country only (blank region)
        if (rules.isEmpty() && region != null && !region.isBlank()) {
            rules = taxRuleRepository.findByCountryAndRegion(normalisedCountry, null);
        }

        // 3. Default 10 % when no rules are configured
        if (rules.isEmpty()) {
            return subtotal.multiply(DEFAULT_TAX_RATE);
        }

        // Filter out zero/negative rates so they don't shadow the default fallback
        List<TaxRule> effectiveRules = rules.stream().filter(r -> r.getRate() != null && r.getRate().signum() > 0)
                .toList();

        if (effectiveRules.isEmpty()) {
            return subtotal.multiply(DEFAULT_TAX_RATE);
        }

        // Pick the rule with the highest rate
        TaxRule bestRule = effectiveRules.stream().max(java.util.Comparator.comparing(TaxRule::getRate)).orElse(null);

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
        taxRuleRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound(ENTITY_TAX_RULE, id));
        taxRuleRepository.delete(id);
    }
}
