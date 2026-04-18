package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.ShippingCarrierDtoIn;
import com.backandwhite.api.dto.in.ShippingRuleDtoIn;
import com.backandwhite.api.dto.in.TaxRuleDtoIn;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.model.TaxRule;
import com.backandwhite.domain.valueobject.TaxType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShippingTaxApiMapperTest {

    private ShippingTaxApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ShippingTaxApiMapperImpl();
        Field f = ShippingTaxApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    // ── Carriers ─────────────────────────────
    @Test
    void toCarrierDto_null() {
        assertThat(mapper.toCarrierDto(null)).isNull();
    }

    @Test
    void toCarrierDto_full() {
        ShippingCarrier c = ShippingCarrier.builder().id("c1").name("DHL").code("DHL").logoUrl("url").active(true)
                .build();
        var dto = mapper.toCarrierDto(c);
        assertThat(dto.getId()).isEqualTo("c1");
        assertThat(dto.getName()).isEqualTo("DHL");
    }

    @Test
    void toCarrierDtoList_null() {
        assertThat(mapper.toCarrierDtoList(null)).isNull();
    }

    @Test
    void toCarrierDtoList_mapsList() {
        assertThat(mapper.toCarrierDtoList(List.of(ShippingCarrier.builder().id("c1").build()))).hasSize(1);
    }

    @Test
    void toCarrierDomain_null() {
        assertThat(mapper.toCarrierDomain(null)).isNull();
    }

    @Test
    void toCarrierDomain_full() {
        ShippingCarrierDtoIn dto = new ShippingCarrierDtoIn();
        dto.setName("DHL");
        dto.setCode("DHL");
        dto.setLogoUrl("url");
        dto.setActive(true);
        ShippingCarrier c = mapper.toCarrierDomain(dto);
        assertThat(c.getName()).isEqualTo("DHL");
        assertThat(c.isActive()).isTrue();
    }

    // ── Rules ────────────────────────────────
    @Test
    void toRuleDto_null() {
        assertThat(mapper.toRuleDto(null)).isNull();
    }

    @Test
    void toRuleDto_full() {
        ShippingRule r = ShippingRule.builder().id("r1").carrierId("c1").zone("US").rate(Money.of(new BigDecimal("5")))
                .freeAbove(Money.of(new BigDecimal("100"))).estimatedDays(3).carrierName("DHL").active(true).build();
        var dto = mapper.toRuleDto(r);
        assertThat(dto.getId()).isEqualTo("r1");
        assertThat(dto.getRate()).isEqualByComparingTo("5");
    }

    @Test
    void toRuleDtoList_null() {
        assertThat(mapper.toRuleDtoList(null)).isNull();
    }

    @Test
    void toRuleDtoList_mapsList() {
        assertThat(mapper.toRuleDtoList(List.of(ShippingRule.builder().id("r1").build()))).hasSize(1);
    }

    @Test
    void toRuleDomain_null() {
        assertThat(mapper.toRuleDomain(null)).isNull();
    }

    @Test
    void toRuleDomain_full() {
        ShippingRuleDtoIn dto = new ShippingRuleDtoIn();
        dto.setCarrierId("c1");
        dto.setZone("US");
        dto.setWeightMin(BigDecimal.ZERO);
        dto.setWeightMax(new BigDecimal("30"));
        dto.setPriceMin(BigDecimal.ZERO);
        dto.setPriceMax(new BigDecimal("1000"));
        dto.setRate(new BigDecimal("5"));
        dto.setFreeAbove(new BigDecimal("100"));
        dto.setEstimatedDays(5);
        dto.setActive(true);

        ShippingRule r = mapper.toRuleDomain(dto);
        assertThat(r.getCarrierId()).isEqualTo("c1");
        assertThat(r.getEstimatedDays()).isEqualTo(5);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    void toRuleDomain_nullActive_defaultsTrue() {
        ShippingRuleDtoIn dto = new ShippingRuleDtoIn();
        dto.setCarrierId("c1");
        dto.setZone("US");
        dto.setRate(new BigDecimal("5"));
        dto.setEstimatedDays(null);
        dto.setActive(null);

        ShippingRule r = mapper.toRuleDomain(dto);
        assertThat(r.isActive()).isTrue();
    }

    // ── Tax ──────────────────────────────────
    @Test
    void toTaxRuleDto_null() {
        assertThat(mapper.toTaxRuleDto(null)).isNull();
    }

    @Test
    void toTaxRuleDto_full() {
        TaxRule r = TaxRule.builder().id("t1").country("US").region("CA").rate(new BigDecimal("0.08"))
                .type(TaxType.PERCENTAGE).active(true).appliesToCategories(List.of("cat1")).build();
        var dto = mapper.toTaxRuleDto(r);
        assertThat(dto.getId()).isEqualTo("t1");
        assertThat(dto.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toTaxRuleDto_nullCategories() {
        TaxRule r = TaxRule.builder().id("t1").appliesToCategories(null).build();
        var dto = mapper.toTaxRuleDto(r);
        assertThat(dto.getAppliesToCategories()).isNull();
    }

    @Test
    void toTaxRuleDtoList_null() {
        assertThat(mapper.toTaxRuleDtoList(null)).isNull();
    }

    @Test
    void toTaxRuleDtoList_mapsList() {
        assertThat(mapper.toTaxRuleDtoList(List.of(TaxRule.builder().id("t1").build()))).hasSize(1);
    }

    @Test
    void toTaxRuleDomain_null() {
        assertThat(mapper.toTaxRuleDomain(null)).isNull();
    }

    @Test
    void toTaxRuleDomain_full() {
        TaxRuleDtoIn dto = new TaxRuleDtoIn();
        dto.setCountry("US");
        dto.setRegion("CA");
        dto.setRate(new BigDecimal("0.08"));
        dto.setType(TaxType.PERCENTAGE);
        dto.setActive(true);
        dto.setAppliesToCategories(List.of("cat1"));

        TaxRule r = mapper.toTaxRuleDomain(dto);
        assertThat(r.getCountry()).isEqualTo("US");
        assertThat(r.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toTaxRuleDomain_nullCategories() {
        TaxRuleDtoIn dto = new TaxRuleDtoIn();
        dto.setCountry("US");
        dto.setType(TaxType.VAT);
        dto.setRate(BigDecimal.ZERO);

        TaxRule r = mapper.toTaxRuleDomain(dto);
        assertThat(r.getAppliesToCategories()).isNull();
    }
}
