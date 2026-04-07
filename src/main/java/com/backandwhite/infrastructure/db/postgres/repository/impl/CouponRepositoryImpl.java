package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.domain.repository.CouponRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.CouponInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CouponJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.CouponUsageJpaRepository;
import com.backandwhite.infrastructure.db.postgres.specification.CouponSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpa;
    private final CouponUsageJpaRepository usageJpa;
    private final CouponInfraMapper mapper;

    @Override
    public Coupon save(Coupon coupon) {
        coupon.setId(UUID.randomUUID().toString());
        return mapper.toDomain(couponJpa.save(mapper.toEntity(coupon)));
    }

    @Override
    public Coupon update(Coupon coupon) {
        return mapper.toDomain(couponJpa.save(mapper.toEntity(coupon)));
    }

    @Override
    public Optional<Coupon> findById(String id) {
        return couponJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponJpa.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public Page<Coupon> findAll(Map<String, Object> filters, Pageable pageable) {
        return couponJpa.findAll(CouponSpecification.withFilters(filters), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(String id) {
        couponJpa.deleteById(id);
    }

    @Override
    public void incrementUsedCount(String id) {
        couponJpa.incrementUsedCount(id);
    }

    @Override
    public int countUsagesByUser(String couponId, String userId) {
        return usageJpa.countByCouponIdAndUserId(couponId, userId);
    }

    @Override
    public CouponUsage saveUsage(CouponUsage usage) {
        usage.setId(UUID.randomUUID().toString());
        return mapper.toUsageDomain(usageJpa.save(mapper.toUsageEntity(usage)));
    }

    @Override
    public List<CouponUsage> findUsagesByCouponId(String couponId) {
        return usageJpa.findByCouponIdOrderByUsedAtDesc(couponId).stream()
                .map(mapper::toUsageDomain)
                .toList();
    }
}
