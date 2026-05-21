package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.infrastructure.db.postgres.entity.TaxRuleEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.ShippingTaxInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.TaxRuleJpaRepository;
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
class TaxRuleRepositoryImplTest {

    @Mock
    private TaxRuleJpaRepository jpa;

    @Mock
    private ShippingTaxInfraMapper mapper;

    @InjectMocks
    private TaxRuleRepositoryImpl adapter;

    private TaxRule rule(String id) {
        return TaxRule.builder().id(id).country("ES").build();
    }

    @Test
    void save_assignsUuid() {
        TaxRule in = rule(null);
        TaxRuleEntity entity = new TaxRuleEntity();
        TaxRuleEntity saved = new TaxRuleEntity();
        TaxRule out = rule("x");

        when(mapper.toTaxEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toTaxDomain(saved)).thenReturn(out);

        TaxRule result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        TaxRule in = rule("kept");
        TaxRuleEntity entity = new TaxRuleEntity();
        TaxRuleEntity saved = new TaxRuleEntity();
        TaxRule out = rule("kept");

        when(mapper.toTaxEntity(in)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(saved);
        when(mapper.toTaxDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        TaxRuleEntity e = new TaxRuleEntity();
        when(jpa.findById("t1")).thenReturn(Optional.of(e));
        when(mapper.toTaxDomain(e)).thenReturn(rule("t1"));
        assertThat(adapter.findById("t1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(jpa.findById("t1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("t1")).isEmpty();
    }

    @Test
    void findAll_delegates() {
        TaxRuleEntity e = new TaxRuleEntity();
        Page<TaxRuleEntity> p = new PageImpl<>(List.of(e));
        when(jpa.findAll(PageRequest.of(0, 5))).thenReturn(p);
        when(mapper.toTaxDomain(e)).thenReturn(rule("t1"));

        assertThat(adapter.findAll(Map.of(), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void findByCountryAndRegion_withRegion_callsRegionVariant() {
        TaxRuleEntity e = new TaxRuleEntity();
        when(jpa.findByCountryAndRegionAndActiveTrue("US", "CA")).thenReturn(List.of(e));
        when(mapper.toTaxDomain(e)).thenReturn(rule("t1"));

        assertThat(adapter.findByCountryAndRegion("US", "CA")).hasSize(1);
    }

    @Test
    void findByCountryAndRegion_nullRegion_callsCountryOnly() {
        TaxRuleEntity e = new TaxRuleEntity();
        when(jpa.findByCountryAndActiveTrue("US")).thenReturn(List.of(e));
        when(mapper.toTaxDomain(e)).thenReturn(rule("t1"));

        assertThat(adapter.findByCountryAndRegion("US", null)).hasSize(1);
    }

    @Test
    void findByCountryAndRegion_blankRegion_callsCountryOnly() {
        TaxRuleEntity e = new TaxRuleEntity();
        when(jpa.findByCountryAndActiveTrue("US")).thenReturn(List.of(e));
        when(mapper.toTaxDomain(e)).thenReturn(rule("t1"));

        assertThat(adapter.findByCountryAndRegion("US", "  ")).hasSize(1);
    }

    @Test
    void delete_delegates() {
        adapter.delete("t1");
        verify(jpa).deleteById("t1");
    }
}
