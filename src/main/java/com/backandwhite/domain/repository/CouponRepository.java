package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);

    Coupon update(Coupon coupon);

    Optional<Coupon> findById(String id);

    Optional<Coupon> findByCode(String code);

    Page<Coupon> findAll(Map<String, Object> filters, Pageable pageable);

    void delete(String id);

    void incrementUsedCount(String id);

    int countUsagesByUser(String couponId, String userId);

    CouponUsage saveUsage(CouponUsage usage);
}
