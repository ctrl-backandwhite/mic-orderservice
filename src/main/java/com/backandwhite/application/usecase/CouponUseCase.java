package com.backandwhite.application.usecase;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.domain.model.Coupon;

import java.math.BigDecimal;
import java.util.Map;

public interface CouponUseCase {
    Coupon create(Coupon coupon);

    Coupon update(String id, Coupon coupon);

    Coupon findById(String id);

    PaginationDtoOut<Coupon> findAll(Map<String, Object> filters, int page, int size, String sortBy, boolean ascending);

    void delete(String id);

    void toggleActive(String id);

    /**
     * Validates a coupon and returns the calculated discount amount.
     */
    BigDecimal validate(String code, BigDecimal cartSubtotal, String userId);

    /**
     * Finds a coupon by code.
     */
    Coupon findByCode(String code);

    /**
     * Applies a coupon to an order — increments usage, records user usage.
     */
    void applyCouponToOrder(String couponId, String userId, String orderId);
}
