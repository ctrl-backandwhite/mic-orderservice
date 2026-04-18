package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.CouponDtoIn;
import com.backandwhite.api.dto.out.CouponDtoOut;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.valueobject.CouponType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CouponApiMapperTest {

    private CouponApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new CouponApiMapperImpl();
        Field f = CouponApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    @Test
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_full_mapsAllFields() {
        Coupon c = Coupon.builder().id("c1").code("SAVE10").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).minOrderAmount(Money.of(new BigDecimal("50"))).maxUses(100)
                .usedCount(5).maxUsesPerUser(1).validFrom(Instant.EPOCH).validUntil(Instant.now()).active(true)
                .appliesToCategories(List.of("cat1")).appliesToProducts(List.of("p1")).build();

        CouponDtoOut dto = mapper.toDto(c);
        assertThat(dto.getId()).isEqualTo("c1");
        assertThat(dto.getCode()).isEqualTo("SAVE10");
        assertThat(dto.getValue()).isEqualByComparingTo("10.00");
        assertThat(dto.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toDtoList_null_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toDtoList_mapsAll() {
        List<CouponDtoOut> list = mapper.toDtoList(List.of(Coupon.builder().id("a").build()));
        assertThat(list).hasSize(1);
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full_mapsAllFields() {
        CouponDtoIn dto = new CouponDtoIn();
        dto.setCode("SAVE10");
        dto.setType(CouponType.PERCENTAGE);
        dto.setValue(new BigDecimal("10"));
        dto.setMinOrderAmount(new BigDecimal("50"));
        dto.setMaxUses(100);
        dto.setMaxUsesPerUser(1);
        dto.setValidFrom(Instant.EPOCH);
        dto.setValidUntil(Instant.now());
        dto.setAppliesToCategories(List.of("cat1"));
        dto.setAppliesToProducts(List.of("p1"));
        dto.setActive(true);

        Coupon c = mapper.toDomain(dto);
        assertThat(c.getCode()).isEqualTo("SAVE10");
        assertThat(c.getValue().getAmount()).isEqualByComparingTo("10.00");
        assertThat(c.getAppliesToProducts()).containsExactly("p1");
    }

    @Test
    void toDomain_nullLists_skipped() {
        CouponDtoIn dto = new CouponDtoIn();
        dto.setCode("C");
        dto.setType(CouponType.FIXED);

        Coupon c = mapper.toDomain(dto);
        assertThat(c.getAppliesToCategories()).isNull();
        assertThat(c.getAppliesToProducts()).isNull();
    }
}
