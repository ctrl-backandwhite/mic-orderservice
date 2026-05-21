package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingRuleEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.ShippingRuleJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ShippingRuleRepositoryImplTest {

    @Mock
    private ShippingRuleJpaRepository jpa;

    @Mock
    private ShippingTaxInfraMapper mapper;

    @InjectMocks
    private ShippingRuleRepositoryImpl adapter;

    private ShippingRule rule(String id) {
        return ShippingRule.builder().id(id).zone("ES").build();
    }

    @Test
    void save_assignsUuid() {
        ShippingRule in = rule(null);
        ShippingRuleEntity entity = new ShippingRuleEntity();
        ShippingRuleEntity saved = new ShippingRuleEntity();
        ShippingRule out = rule("x");

        when(mapper.toRuleEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toRuleDomain(saved)).thenReturn(out);

        ShippingRule result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        ShippingRule in = rule("kept");
        ShippingRuleEntity entity = new ShippingRuleEntity();
        ShippingRuleEntity saved = new ShippingRuleEntity();
        ShippingRule out = rule("kept");

        when(mapper.toRuleEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toRuleDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        ShippingRuleEntity e = new ShippingRuleEntity();
        when(jpa.findById("r1")).thenReturn(Optional.of(e));
        when(mapper.toRuleDomain(e)).thenReturn(rule("r1"));
        assertThat(adapter.findById("r1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(jpa.findById("r1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("r1")).isEmpty();
    }

    @Test
    void findAll_delegates() {
        ShippingRuleEntity e = new ShippingRuleEntity();
        Page<ShippingRuleEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findAll(PageRequest.of(0, 5))).thenReturn(p);
        when(mapper.toRuleDomain(e)).thenReturn(rule("r1"));

        assertThat(adapter.findAll(Map.of(), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void findOptions_freeAboveTriggered_setsRateZero() {
        ShippingRuleEntity e = new ShippingRuleEntity();
        ShippingRule r = ShippingRule.builder().id("r1").zone("ES").rate(Money.of(new BigDecimal("9.99")))
                .freeAbove(Money.of(new BigDecimal("50.00"))).build();
        when(jpa.findApplicableRules("ES", new BigDecimal("1.0"), new BigDecimal("100.00"))).thenReturn(List.of(e));
        when(mapper.toRuleDomain(e)).thenReturn(r);

        List<ShippingRule> result = adapter.findOptions("ES", new BigDecimal("1.0"),
                Money.of(new BigDecimal("100.00")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRate().isZero()).isTrue();
    }

    @Test
    void findOptions_freeAboveNotMet_keepsRate() {
        ShippingRuleEntity e = new ShippingRuleEntity();
        ShippingRule r = ShippingRule.builder().id("r1").zone("ES").rate(Money.of(new BigDecimal("9.99")))
                .freeAbove(Money.of(new BigDecimal("50.00"))).build();
        when(jpa.findApplicableRules("ES", new BigDecimal("1.0"), new BigDecimal("10.00"))).thenReturn(List.of(e));
        when(mapper.toRuleDomain(e)).thenReturn(r);

        List<ShippingRule> result = adapter.findOptions("ES", new BigDecimal("1.0"), Money.of(new BigDecimal("10.00")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRate().getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    void findOptions_noFreeAbove_keepsRate() {
        ShippingRuleEntity e = new ShippingRuleEntity();
        ShippingRule r = ShippingRule.builder().id("r1").zone("ES").rate(Money.of(new BigDecimal("9.99")))
                .freeAbove(null).build();
        when(jpa.findApplicableRules("ES", new BigDecimal("1.0"), new BigDecimal("10.00"))).thenReturn(List.of(e));
        when(mapper.toRuleDomain(e)).thenReturn(r);

        List<ShippingRule> result = adapter.findOptions("ES", new BigDecimal("1.0"), Money.of(new BigDecimal("10.00")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRate().getAmount()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    void delete_delegates() {
        adapter.delete("r1");
        verify(jpa).deleteById("r1");
    }
}
