package com.backandwhite.application.usecase.impl;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.*;

import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.domain.repository.CouponRepository;
import com.backandwhite.domain.valueobject.CouponType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        couponRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
        coupon.setId(id);
        return couponRepository.update(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon findById(String id) {
        return couponRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
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
        couponRepository.findById(id).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", id));
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
        return couponRepository.findByCode(code).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", code));
    }

    @Override
    @Transactional(readOnly = true)
    public Money validate(String code, Money cartSubtotal, String userId) {
        if (cartSubtotal == null) {
            throw new IllegalArgumentException("cartSubtotal is required");
        }
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
        if (coupon.getMinOrderAmount() != null && cartSubtotal.isLessThan(coupon.getMinOrderAmount())) {
            throw COUPON_MIN_ORDER.toBusinessException(coupon.getMinOrderAmount().getAmount());
        }
        if (userId != null) {
            int maxPerUser = coupon.getMaxUsesPerUser() != null ? coupon.getMaxUsesPerUser() : 1;
            int userUsages = couponRepository.countUsagesByUser(coupon.getId(), userId);
            if (userUsages >= maxPerUser) {
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
            couponRepository.saveUsage(CouponUsage.builder().couponId(couponId).userId(userId).orderId(orderId)
                    .usedAt(Instant.now()).build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsage> findUsages(String couponId) {
        couponRepository.findById(couponId).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Coupon", couponId));
        return couponRepository.findUsagesByCouponId(couponId);
    }

    private Money calculateDiscount(Coupon coupon, Money subtotal) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return subtotal.percentage(coupon.getValue().getAmount());
        }
        if (coupon.getType() == CouponType.FIXED) {
            return coupon.getValue().min(subtotal);
        }
        // FREE_SHIPPING — discount is 0, but shipping will be zeroed by order service
        return Money.zero();
    }
}
