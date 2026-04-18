package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.domain.valueobject.CouponType;
import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CouponUsageEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CouponInfraMapperTest {

    private CouponInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new CouponInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        CouponEntity entity = CouponEntity.builder().id("c1").code("SAVE10").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).validFrom(Instant.EPOCH).validUntil(Instant.now())
                .appliesToCategories(List.of("cat1")).appliesToProducts(List.of("p1")).active(true).build();
        Coupon d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("c1");
        assertThat(d.getAppliesToCategories()).containsExactly("cat1");
    }

    @Test
    void toDomain_nullLists() {
        CouponEntity entity = CouponEntity.builder().id("c1").appliesToCategories(null).appliesToProducts(null).build();
        Coupon d = mapper.toDomain(entity);
        assertThat(d.getAppliesToCategories()).isNull();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        Coupon d = Coupon.builder().id("c1").code("SAVE10").type(CouponType.PERCENTAGE)
                .appliesToCategories(List.of("cat1")).appliesToProducts(List.of("p1")).active(true).build();
        CouponEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("c1");
        assertThat(e.getAppliesToProducts()).containsExactly("p1");
    }

    @Test
    void toEntity_nullLists() {
        Coupon d = Coupon.builder().id("c1").appliesToCategories(null).appliesToProducts(null).build();
        CouponEntity e = mapper.toEntity(d);
        assertThat(e.getAppliesToCategories()).isNull();
    }

    @Test
    void toUsageDomain_null() {
        assertThat(mapper.toUsageDomain(null)).isNull();
    }

    @Test
    void toUsageDomain_full() {
        CouponUsageEntity entity = CouponUsageEntity.builder().id("u1").couponId("c1").userId("u1").orderId("o1")
                .usedAt(Instant.EPOCH).build();
        CouponUsage d = mapper.toUsageDomain(entity);
        assertThat(d.getId()).isEqualTo("u1");
    }

    @Test
    void toUsageEntity_null() {
        assertThat(mapper.toUsageEntity(null)).isNull();
    }

    @Test
    void toUsageEntity_full() {
        CouponUsage d = CouponUsage.builder().id("u1").couponId("c1").userId("u1").orderId("o1").build();
        CouponUsageEntity e = mapper.toUsageEntity(d);
        assertThat(e.getId()).isEqualTo("u1");
    }
}
