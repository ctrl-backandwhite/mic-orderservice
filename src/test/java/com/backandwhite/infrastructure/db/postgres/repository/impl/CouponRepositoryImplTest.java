package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CouponUsageEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.CouponInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CouponJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.CouponUsageJpaRepository;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CouponRepositoryImplTest {

    @Mock
    private CouponJpaRepository couponJpa;

    @Mock
    private CouponUsageJpaRepository usageJpa;

    @Mock
    private CouponInfraMapper mapper;

    @InjectMocks
    private CouponRepositoryImpl adapter;

    private Coupon coupon(String id) {
        return Coupon.builder().id(id).code("C").build();
    }

    private CouponUsage usage(String id) {
        return CouponUsage.builder().id(id).couponId("c1").userId("u1").build();
    }

    @Test
    void save_assignsUuid() {
        Coupon in = coupon(null);
        CouponEntity entity = new CouponEntity();
        CouponEntity saved = new CouponEntity();
        Coupon out = coupon("x");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(couponJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Coupon result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_keepsId() {
        Coupon in = coupon("kept");
        CouponEntity entity = new CouponEntity();
        CouponEntity saved = new CouponEntity();
        Coupon out = coupon("kept");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(couponJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        assertThat(adapter.update(in)).isSameAs(out);
        assertThat(in.getId()).isEqualTo("kept");
    }

    @Test
    void findById_present() {
        CouponEntity e = new CouponEntity();
        when(couponJpa.findById("c1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(coupon("c1"));
        assertThat(adapter.findById("c1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(couponJpa.findById("c1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("c1")).isEmpty();
    }

    @Test
    void findByCode_present() {
        CouponEntity e = new CouponEntity();
        when(couponJpa.findByCode("CODE")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(coupon("c1"));
        assertThat(adapter.findByCode("CODE")).isPresent();
    }

    @Test
    void findByCode_empty() {
        when(couponJpa.findByCode("CODE")).thenReturn(Optional.empty());
        assertThat(adapter.findByCode("CODE")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_delegates() {
        CouponEntity e = new CouponEntity();
        Page<CouponEntity> p = new PageImpl<>(List.of(e));
        when(couponJpa.findAll(any(Specification.class), eq(PageRequest.of(0, 5)))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(coupon("c1"));

        assertThat(adapter.findAll(Map.of(), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    void delete_delegates() {
        adapter.delete("c1");
        verify(couponJpa).deleteById("c1");
    }

    @Test
    void incrementUsedCount_delegates() {
        adapter.incrementUsedCount("c1");
        verify(couponJpa).incrementUsedCount("c1");
    }

    @Test
    void countUsagesByUser_delegates() {
        when(usageJpa.countByCouponIdAndUserId("c1", "u1")).thenReturn(7);
        assertThat(adapter.countUsagesByUser("c1", "u1")).isEqualTo(7);
    }

    @Test
    void saveUsage_assignsUuid() {
        CouponUsage in = usage(null);
        CouponUsageEntity entity = new CouponUsageEntity();
        CouponUsageEntity saved = new CouponUsageEntity();
        CouponUsage out = usage("x");

        when(mapper.toUsageEntity(in)).thenReturn(entity);
        when(usageJpa.save(entity)).thenReturn(saved);
        when(mapper.toUsageDomain(saved)).thenReturn(out);

        CouponUsage result = adapter.saveUsage(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
    }

    @Test
    void findUsagesByCouponId_delegates() {
        CouponUsageEntity e = new CouponUsageEntity();
        when(usageJpa.findByCouponIdOrderByUsedAtDesc("c1")).thenReturn(List.of(e));
        when(mapper.toUsageDomain(e)).thenReturn(usage("u1"));

        assertThat(adapter.findUsagesByCouponId("c1")).hasSize(1);
    }
}
