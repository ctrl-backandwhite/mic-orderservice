package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.domain.repository.CouponRepository;
import com.backandwhite.domain.valueobject.CouponType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class CouponUseCaseImpl implements CouponUseCase {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public Coupon create(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public Coupon update(String id, Coupon coupon) {
        couponRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
        coupon.setId(id);
        return couponRepository.update(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon findById(String id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Coupon> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(couponRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional
    public void delete(String id) {
        couponRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
        couponRepository.delete(id);
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
        coupon.setActive(!coupon.isActive());
        couponRepository.update(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon findByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", code));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal validate(String code, BigDecimal cartSubtotal, String userId) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", code));

        if (!coupon.isActive()) {
            throw COUPON_INACTIVE.toBusinessException();
        }

        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom())) {
            throw COUPON_NOT_YET_VALID.toBusinessException();
        }
        if (now.isAfter(coupon.getValidUntil())) {
            throw COUPON_EXPIRED.toBusinessException();
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw COUPON_EXHAUSTED.toBusinessException();
        }
        if (coupon.getMinOrderAmount() != null && cartSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw COUPON_MIN_ORDER.toBusinessException(coupon.getMinOrderAmount());
        }
        if (userId != null && coupon.getMaxUsesPerUser() != null) {
            int userUsages = couponRepository.countUsagesByUser(coupon.getId(), userId);
            if (userUsages >= coupon.getMaxUsesPerUser()) {
                throw COUPON_USER_LIMIT.toBusinessException();
            }
        }

        return calculateDiscount(coupon, cartSubtotal);
    }

    @Override
    @Transactional
    public void applyCouponToOrder(String couponId, String userId, String orderId) {
        couponRepository.incrementUsedCount(couponId);
        if (userId != null) {
            couponRepository.saveUsage(CouponUsage.builder()
                    .couponId(couponId)
                    .userId(userId)
                    .orderId(orderId)
                    .usedAt(Instant.now())
                    .build());
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (coupon.getType() == CouponType.FIXED) {
            return coupon.getValue().min(subtotal);
        }
        // FREE_SHIPPING — discount is 0, but shipping will be zeroed by order service
        return BigDecimal.ZERO;
    }
}
