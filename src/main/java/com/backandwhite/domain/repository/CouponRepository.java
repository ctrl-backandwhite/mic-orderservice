package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<CouponUsage> findUsagesByCouponId(String couponId);
}
