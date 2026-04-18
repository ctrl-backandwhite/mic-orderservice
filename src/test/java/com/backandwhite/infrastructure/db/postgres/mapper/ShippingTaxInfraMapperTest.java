package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.valueobject.TaxType;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingCarrierEntity;
import com.backandwhite.infrastructure.db.postgres.entity.ShippingRuleEntity;
import com.backandwhite.infrastructure.db.postgres.entity.TaxRuleEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShippingTaxInfraMapperTest {

    private ShippingTaxInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new ShippingTaxInfraMapperImpl();
    }

    @Test
    void toCarrierDomain_null() {
        assertThat(mapper.toCarrierDomain(null)).isNull();
    }

    @Test
    void toCarrierDomain_full() {
        ShippingCarrierEntity entity = ShippingCarrierEntity.builder().id("c1").name("DHL").code("DHL").logoUrl("url")
                .active(true).build();
        ShippingCarrier d = mapper.toCarrierDomain(entity);
        assertThat(d.getId()).isEqualTo("c1");
    }

    @Test
    void toCarrierEntity_null() {
        assertThat(mapper.toCarrierEntity(null)).isNull();
    }

    @Test
    void toCarrierEntity_full() {
        ShippingCarrier d = ShippingCarrier.builder().id("c1").name("DHL").code("DHL").build();
        ShippingCarrierEntity e = mapper.toCarrierEntity(d);
        assertThat(e.getId()).isEqualTo("c1");
    }

    @Test
    void toRuleDomain_null() {
        assertThat(mapper.toRuleDomain(null)).isNull();
    }

    @Test
    void toRuleDomain_withCarrier() {
        ShippingCarrierEntity carrier = ShippingCarrierEntity.builder().id("c1").name("DHL").build();
        ShippingRuleEntity entity = ShippingRuleEntity.builder().id("r1").carrierId("c1").carrier(carrier).zone("US")
                .rate(Money.of(new BigDecimal("5"))).estimatedDays(3).active(true).build();
        ShippingRule d = mapper.toRuleDomain(entity);
        assertThat(d.getId()).isEqualTo("r1");
        assertThat(d.getCarrierName()).isEqualTo("DHL");
    }

    @Test
    void toRuleDomain_nullCarrier() {
        ShippingRuleEntity entity = ShippingRuleEntity.builder().id("r1").carrier(null).build();
        ShippingRule d = mapper.toRuleDomain(entity);
        assertThat(d.getCarrierName()).isNull();
    }

    @Test
    void toRuleEntity_null() {
        assertThat(mapper.toRuleEntity(null)).isNull();
    }

    @Test
    void toRuleEntity_full() {
        ShippingRule d = ShippingRule.builder().id("r1").carrierId("c1").zone("US").rate(Money.of(new BigDecimal("5")))
                .active(true).build();
        ShippingRuleEntity e = mapper.toRuleEntity(d);
        assertThat(e.getId()).isEqualTo("r1");
    }

    @Test
    void toTaxDomain_null() {
        assertThat(mapper.toTaxDomain(null)).isNull();
    }

    @Test
    void toTaxDomain_full() {
        TaxRuleEntity entity = TaxRuleEntity.builder().id("t1").country("US").region("CA").rate(new BigDecimal("0.08"))
                .type(TaxType.PERCENTAGE).appliesToCategories(List.of("cat1")).active(true).build();
        TaxRule d = mapper.toTaxDomain(entity);
        assertThat(d.getId()).isEqualTo("t1");
        assertThat(d.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toTaxDomain_nullCategories() {
        TaxRuleEntity entity = TaxRuleEntity.builder().id("t1").appliesToCategories(null).build();
        TaxRule d = mapper.toTaxDomain(entity);
        assertThat(d.getAppliesToCategories()).isNull();
    }

    @Test
    void toTaxEntity_null() {
        assertThat(mapper.toTaxEntity(null)).isNull();
    }

    @Test
    void toTaxEntity_full() {
        TaxRule d = TaxRule.builder().id("t1").country("US").appliesToCategories(List.of("cat1")).build();
        TaxRuleEntity e = mapper.toTaxEntity(d);
        assertThat(e.getId()).isEqualTo("t1");
        assertThat(e.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toTaxEntity_nullCategories() {
        TaxRule d = TaxRule.builder().id("t1").appliesToCategories(null).build();
        TaxRuleEntity e = mapper.toTaxEntity(d);
        assertThat(e.getAppliesToCategories()).isNull();
    }
}
