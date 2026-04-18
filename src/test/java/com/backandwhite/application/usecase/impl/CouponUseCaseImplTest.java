package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.exception.BusinessException;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import com.backandwhite.domain.repository.CouponRepository;
import com.backandwhite.domain.valueobject.CouponType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CouponUseCaseImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponUseCaseImpl useCase;

    private Coupon.CouponBuilder baseValidCoupon() {
        return Coupon.builder().id("c1").code("SAVE10").active(true).type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void create_delegatesToRepository() {
        Coupon coupon = baseValidCoupon().build();
        when(couponRepository.save(coupon)).thenReturn(coupon);
        assertThat(useCase.create(coupon)).isSameAs(coupon);
    }

    @Test
    void update_existing_updatesId() {
        Coupon existing = baseValidCoupon().build();
        Coupon newValues = Coupon.builder().code("SAVE20").build();
        when(couponRepository.findById("c1")).thenReturn(Optional.of(existing));
        when(couponRepository.update(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        Coupon result = useCase.update("c1", newValues);

        assertThat(result.getId()).isEqualTo("c1");
    }

    @Test
    void update_missing_throws() {
        when(couponRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.update("x", Coupon.builder().build()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findById_existing_returns() {
        Coupon c = baseValidCoupon().build();
        when(couponRepository.findById("c1")).thenReturn(Optional.of(c));
        assertThat(useCase.findById("c1")).isSameAs(c);
    }

    @Test
    void findById_missing_throws() {
        when(couponRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_ascending() {
        Page<Coupon> page = new PageImpl<>(List.of(baseValidCoupon().build()));
        when(couponRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        var result = useCase.findAll(Map.of(), 0, 10, "code", true);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void findAll_descending() {
        Page<Coupon> page = new PageImpl<>(List.of());
        when(couponRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        var result = useCase.findAll(Map.of(), 0, 10, "code", false);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void delete_existing_deletes() {
        when(couponRepository.findById("c1")).thenReturn(Optional.of(baseValidCoupon().build()));
        useCase.delete("c1");
        verify(couponRepository).delete("c1");
    }

    @Test
    void delete_missing_throws() {
        when(couponRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.delete("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void toggleActive_active_togglesToInactive() {
        Coupon c = baseValidCoupon().active(true).build();
        when(couponRepository.findById("c1")).thenReturn(Optional.of(c));
        useCase.toggleActive("c1");
        assertThat(c.isActive()).isFalse();
        verify(couponRepository).update(c);
    }

    @Test
    void toggleActive_inactive_togglesToActive() {
        Coupon c = baseValidCoupon().active(false).build();
        when(couponRepository.findById("c1")).thenReturn(Optional.of(c));
        useCase.toggleActive("c1");
        assertThat(c.isActive()).isTrue();
    }

    @Test
    void toggleActive_missing_throws() {
        when(couponRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.toggleActive("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByCode_existing_returns() {
        Coupon c = baseValidCoupon().build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThat(useCase.findByCode("SAVE10")).isSameAs(c);
    }

    @Test
    void findByCode_missing_throws() {
        when(couponRepository.findByCode("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findByCode("X")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void validate_nullCartSubtotal_throwsIllegalArgument() {
        assertThatThrownBy(() -> useCase.validate("X", null, "u1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_missingCoupon_throws() {
        when(couponRepository.findByCode("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.validate("X", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void validate_inactive_throws() {
        Coupon c = baseValidCoupon().active(false).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_notYetValid_throws() {
        Coupon c = baseValidCoupon().validFrom(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_expired_throws() {
        Coupon c = baseValidCoupon().validUntil(Instant.now().minus(1, ChronoUnit.DAYS)).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_exhausted_throws() {
        Coupon c = baseValidCoupon().maxUses(5).usedCount(5).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_belowMinOrder_throws() {
        Coupon c = baseValidCoupon().minOrderAmount(Money.of(new BigDecimal("500"))).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_userLimitReached_throws() {
        Coupon c = baseValidCoupon().maxUsesPerUser(2).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        when(couponRepository.countUsagesByUser("c1", "u1")).thenReturn(2);
        assertThatThrownBy(() -> useCase.validate("SAVE10", Money.of(new BigDecimal("100")), "u1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_percentage_returnsDiscount() {
        Coupon c = baseValidCoupon().type(CouponType.PERCENTAGE).value(Money.of(new BigDecimal("10"))).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));

        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("200")), null);
        assertThat(discount.getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void validate_fixed_capsAtSubtotal() {
        Coupon c = baseValidCoupon().type(CouponType.FIXED).value(Money.of(new BigDecimal("50"))).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));

        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("20")), null);
        assertThat(discount.getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void validate_freeShipping_returnsZero() {
        Coupon c = baseValidCoupon().type(CouponType.FREE_SHIPPING).value(Money.of(new BigDecimal("0"))).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));

        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null);
        assertThat(discount.isZero()).isTrue();
    }

    @Test
    void validate_withUserIdDefaultLimit_allowsFirstUse() {
        Coupon c = baseValidCoupon().maxUsesPerUser(null).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        when(couponRepository.countUsagesByUser("c1", "u1")).thenReturn(0);

        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("100")), "u1");
        assertThat(discount).isNotNull();
    }

    @Test
    void applyCouponToOrder_withUser_savesUsage() {
        useCase.applyCouponToOrder("c1", "u1", "o1");
        verify(couponRepository).incrementUsedCount("c1");
        verify(couponRepository).saveUsage(any(CouponUsage.class));
    }

    @Test
    void applyCouponToOrder_withoutUser_doesNotSaveUsage() {
        useCase.applyCouponToOrder("c1", null, "o1");
        verify(couponRepository).incrementUsedCount("c1");
        verify(couponRepository, never()).saveUsage(any());
    }

    @Test
    void findUsages_existing_returnsList() {
        when(couponRepository.findById("c1")).thenReturn(Optional.of(baseValidCoupon().build()));
        when(couponRepository.findUsagesByCouponId("c1")).thenReturn(List.of(CouponUsage.builder().id("u1").build()));
        assertThat(useCase.findUsages("c1")).hasSize(1);
    }

    @Test
    void findUsages_missingCoupon_throws() {
        when(couponRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findUsages("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void validate_maxUsesNull_allowsAny() {
        Coupon c = baseValidCoupon().maxUses(null).usedCount(1000).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null);
        assertThat(discount).isNotNull();
    }

    @Test
    void validate_userIdNull_skipsUserCheck() {
        Coupon c = baseValidCoupon().maxUsesPerUser(1).build();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(c));
        Money discount = useCase.validate("SAVE10", Money.of(new BigDecimal("100")), null);
        assertThat(discount).isNotNull();
        verify(couponRepository, never()).countUsagesByUser(anyString(), eq("ignored"));
    }
}
