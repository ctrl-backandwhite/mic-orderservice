package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.repository.ShippingCarrierRepository;
import com.backandwhite.domain.repository.ShippingRuleRepository;
import com.backandwhite.domain.repository.TaxRuleRepository;
import com.backandwhite.domain.valueobject.TaxType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ShippingTaxUseCaseImplTest {

    @Mock
    private ShippingCarrierRepository carrierRepository;

    @Mock
    private ShippingRuleRepository ruleRepository;

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @InjectMocks
    private ShippingTaxUseCaseImpl useCase;

    // ── Carriers ─────────────────────────────────────────
    @Test
    void createCarrier_delegates() {
        ShippingCarrier c = ShippingCarrier.builder().name("DHL").build();
        when(carrierRepository.save(c)).thenReturn(c);
        assertThat(useCase.createCarrier(c)).isSameAs(c);
    }

    @Test
    void updateCarrier_existing() {
        ShippingCarrier c = ShippingCarrier.builder().id("c1").name("DHL").build();
        when(carrierRepository.findById("c1")).thenReturn(Optional.of(c));
        when(carrierRepository.update(any(ShippingCarrier.class))).thenAnswer(inv -> inv.getArgument(0));

        ShippingCarrier updated = useCase.updateCarrier("c1", ShippingCarrier.builder().name("FEDEX").build());
        assertThat(updated.getId()).isEqualTo("c1");
    }

    @Test
    void updateCarrier_missing_throws() {
        when(carrierRepository.findById("x")).thenReturn(Optional.empty());
        ShippingCarrier empty = ShippingCarrier.builder().build();
        assertThatThrownBy(() -> useCase.updateCarrier("x", empty)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findCarrierById_existing() {
        ShippingCarrier c = ShippingCarrier.builder().id("c1").build();
        when(carrierRepository.findById("c1")).thenReturn(Optional.of(c));
        assertThat(useCase.findCarrierById("c1")).isSameAs(c);
    }

    @Test
    void findCarrierById_missing_throws() {
        when(carrierRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findCarrierById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllCarriers_asc() {
        Page<ShippingCarrier> page = new PageImpl<>(List.of(ShippingCarrier.builder().id("c1").build()));
        when(carrierRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllCarriers(0, 10, "name", true).content()).hasSize(1);
    }

    @Test
    void findAllCarriers_desc() {
        Page<ShippingCarrier> page = new PageImpl<>(List.of());
        when(carrierRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllCarriers(0, 10, "name", false).content()).isEmpty();
    }

    @Test
    void deleteCarrier_existing() {
        when(carrierRepository.findById("c1")).thenReturn(Optional.of(ShippingCarrier.builder().id("c1").build()));
        useCase.deleteCarrier("c1");
        verify(carrierRepository).delete("c1");
    }

    @Test
    void deleteCarrier_missing_throws() {
        when(carrierRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.deleteCarrier("x")).isInstanceOf(EntityNotFoundException.class);
    }

    // ── Rules ────────────────────────────────────────────
    @Test
    void createRule_delegates() {
        ShippingRule rule = ShippingRule.builder().zone("US").build();
        when(ruleRepository.save(rule)).thenReturn(rule);
        assertThat(useCase.createRule(rule)).isSameAs(rule);
    }

    @Test
    void updateRule_existing() {
        ShippingRule rule = ShippingRule.builder().id("r1").build();
        when(ruleRepository.findById("r1")).thenReturn(Optional.of(rule));
        when(ruleRepository.update(any(ShippingRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ShippingRule updated = useCase.updateRule("r1", ShippingRule.builder().build());
        assertThat(updated.getId()).isEqualTo("r1");
    }

    @Test
    void updateRule_missing_throws() {
        when(ruleRepository.findById("x")).thenReturn(Optional.empty());
        ShippingRule empty = ShippingRule.builder().build();
        assertThatThrownBy(() -> useCase.updateRule("x", empty)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findRuleById_existing() {
        ShippingRule r = ShippingRule.builder().id("r1").build();
        when(ruleRepository.findById("r1")).thenReturn(Optional.of(r));
        assertThat(useCase.findRuleById("r1")).isSameAs(r);
    }

    @Test
    void findRuleById_missing_throws() {
        when(ruleRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findRuleById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllRules_asc() {
        Page<ShippingRule> page = new PageImpl<>(List.of(ShippingRule.builder().id("r1").build()));
        when(ruleRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllRules(0, 10, "id", true).content()).hasSize(1);
    }

    @Test
    void findAllRules_desc() {
        Page<ShippingRule> page = new PageImpl<>(List.of());
        when(ruleRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllRules(0, 10, "id", false).content()).isEmpty();
    }

    @Test
    void findShippingOptions_nullCountry_returnsEmpty() {
        List<ShippingRule> result = useCase.findShippingOptions(null, BigDecimal.ONE, Money.of(1));
        assertThat(result).isEmpty();
    }

    @Test
    void findShippingOptions_blankCountry_returnsEmpty() {
        List<ShippingRule> result = useCase.findShippingOptions("  ", BigDecimal.ONE, Money.of(1));
        assertThat(result).isEmpty();
    }

    @Test
    void findShippingOptions_validCountry_delegates() {
        ShippingRule rule = ShippingRule.builder().id("r1").zone("US").build();
        when(ruleRepository.findOptions(eq("US"), any(BigDecimal.class), any(Money.class))).thenReturn(List.of(rule));
        List<ShippingRule> result = useCase.findShippingOptions("us", BigDecimal.ONE, Money.of(100));
        assertThat(result).hasSize(1);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void deleteRule_existing() {
        when(ruleRepository.findById("r1")).thenReturn(Optional.of(ShippingRule.builder().id("r1").build()));
        useCase.deleteRule("r1");
        verify(ruleRepository).delete("r1");
    }

    @Test
    void deleteRule_missing_throws() {
        when(ruleRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.deleteRule("x")).isInstanceOf(EntityNotFoundException.class);
    }

    // ── Tax ──────────────────────────────────────────────
    @Test
    void createTaxRule_delegates() {
        TaxRule r = TaxRule.builder().country("US").build();
        when(taxRuleRepository.save(r)).thenReturn(r);
        assertThat(useCase.createTaxRule(r)).isSameAs(r);
    }

    @Test
    void updateTaxRule_existing() {
        TaxRule r = TaxRule.builder().id("t1").build();
        when(taxRuleRepository.findById("t1")).thenReturn(Optional.of(r));
        when(taxRuleRepository.update(any(TaxRule.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxRule updated = useCase.updateTaxRule("t1", TaxRule.builder().build());
        assertThat(updated.getId()).isEqualTo("t1");
    }

    @Test
    void updateTaxRule_missing_throws() {
        when(taxRuleRepository.findById("x")).thenReturn(Optional.empty());
        TaxRule empty = TaxRule.builder().build();
        assertThatThrownBy(() -> useCase.updateTaxRule("x", empty)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findTaxRuleById_existing() {
        TaxRule r = TaxRule.builder().id("t1").build();
        when(taxRuleRepository.findById("t1")).thenReturn(Optional.of(r));
        assertThat(useCase.findTaxRuleById("t1")).isSameAs(r);
    }

    @Test
    void findTaxRuleById_missing_throws() {
        when(taxRuleRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findTaxRuleById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllTaxRules_asc() {
        Page<TaxRule> page = new PageImpl<>(List.of(TaxRule.builder().id("t1").build()));
        when(taxRuleRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllTaxRules(0, 10, "country", true).content()).hasSize(1);
    }

    @Test
    void findAllTaxRules_desc() {
        Page<TaxRule> page = new PageImpl<>(List.of());
        when(taxRuleRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAllTaxRules(0, 10, "country", false).content()).isEmpty();
    }

    @Test
    void calculateTax_noRules_usesDefault() {
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of());
        when(taxRuleRepository.findByCountryAndRegion("US", null)).thenReturn(List.of());

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculateTax_withRegionMatch_usesHighestRate() {
        TaxRule r1 = TaxRule.builder().rate(new BigDecimal("0.05")).type(TaxType.PERCENTAGE).build();
        TaxRule r2 = TaxRule.builder().rate(new BigDecimal("0.15")).type(TaxType.PERCENTAGE).build();
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of(r1, r2));

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void calculateTax_noRegionMatch_fallsBackToCountry() {
        TaxRule r = TaxRule.builder().rate(new BigDecimal("0.08")).type(TaxType.VAT).build();
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of());
        when(taxRuleRepository.findByCountryAndRegion("US", null)).thenReturn(List.of(r));

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    void calculateTax_fixedType_returnsFlatAmount() {
        TaxRule r = TaxRule.builder().rate(new BigDecimal("5.00")).type(TaxType.FIXED).build();
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of(r));

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("200")));
        assertThat(tax.getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void calculateTax_nullRegion_fallbackNotTriggered() {
        when(taxRuleRepository.findByCountryAndRegion("US", null)).thenReturn(List.of());

        Money tax = useCase.calculateTax("US", null, Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void deleteTaxRule_existing() {
        when(taxRuleRepository.findById("t1")).thenReturn(Optional.of(TaxRule.builder().id("t1").build()));
        useCase.deleteTaxRule("t1");
        verify(taxRuleRepository).delete("t1");
    }

    @Test
    void deleteTaxRule_missing_throws() {
        when(taxRuleRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.deleteTaxRule("x")).isInstanceOf(EntityNotFoundException.class);
    }

    // ── extra branch coverage for calculateTax ─────────────
    @Test
    void calculateTax_blankRegion_skipsCountryFallback() {
        when(taxRuleRepository.findByCountryAndRegion("US", "")).thenReturn(List.of());
        Money tax = useCase.calculateTax("US", "", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculateTax_allRulesZeroRate_returnsDefault() {
        // covers: effectiveRules.isEmpty() → default
        TaxRule r1 = TaxRule.builder().rate(new BigDecimal("0")).type(TaxType.PERCENTAGE).build();
        TaxRule r2 = TaxRule.builder().rate(null).type(TaxType.PERCENTAGE).build();
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of(r1, r2));

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculateTax_negativeRate_filteredOut() {
        // covers: rate != null && signum > 0 → false (negative)
        TaxRule negative = TaxRule.builder().rate(new BigDecimal("-0.05")).type(TaxType.PERCENTAGE).build();
        TaxRule positive = TaxRule.builder().rate(new BigDecimal("0.20")).type(TaxType.PERCENTAGE).build();
        when(taxRuleRepository.findByCountryAndRegion("US", "CA")).thenReturn(List.of(negative, positive));

        Money tax = useCase.calculateTax("US", "CA", Money.of(new BigDecimal("100")));
        assertThat(tax.getAmount()).isEqualByComparingTo("20.00");
    }
}
