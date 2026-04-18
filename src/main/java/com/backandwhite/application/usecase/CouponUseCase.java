package com.backandwhite.application.usecase;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import java.util.List;
import java.util.Map;

public interface CouponUseCase {
    Coupon create(Coupon coupon);

    Coupon update(String id, Coupon coupon);

    Coupon findById(String id);

    PageResult<Coupon> findAll(Map<String, Object> filters, int page, int size, String sortBy, boolean ascending);

    void delete(String id);

    void toggleActive(String id);

    /**
     * Validates a coupon and returns the calculated discount amount.
     */
    Money validate(String code, Money cartSubtotal, String userId);

    /**
     * Finds a coupon by code.
     */
    Coupon findByCode(String code);

    /**
     * Applies a coupon to an order — increments usage, records user usage.
     */
    void applyCouponToOrder(String couponId, String userId, String orderId);

    /**
     * Returns the usage history for a coupon.
     */
    List<CouponUsage> findUsages(String couponId);
}
