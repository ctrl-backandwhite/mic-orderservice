package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.CouponDtoIn;
import com.backandwhite.api.dto.in.ValidateCouponDtoIn;
import com.backandwhite.api.dto.out.CouponDtoOut;
import com.backandwhite.api.mapper.CouponApiMapper;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.CouponUsage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponUseCase couponUseCase;
    @Mock
    private CouponApiMapper couponApiMapper;

    @InjectMocks
    private CouponController controller;

    @Test
    void validate_valid_returnsValidPayload() {
        ValidateCouponDtoIn in = ValidateCouponDtoIn.builder().code("WELCOME10").cartSubtotal(new BigDecimal("100"))
                .build();
        when(couponUseCase.validate(eq("WELCOME10"), any(Money.class), eq("u1")))
                .thenReturn(Money.of(new BigDecimal("10")));
        var resp = controller.validate("auth", "u1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().isValid()).isTrue();
        assertThat(resp.getBody().getDiscount()).isEqualByComparingTo("10");
    }

    @Test
    void validate_invalid_returnsInvalidPayload() {
        ValidateCouponDtoIn in = ValidateCouponDtoIn.builder().code("BAD").cartSubtotal(new BigDecimal("100")).build();
        when(couponUseCase.validate(any(), any(), any())).thenThrow(new IllegalArgumentException("expired"));
        var resp = controller.validate("auth", "u1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().isValid()).isFalse();
        assertThat(resp.getBody().getMessage()).isEqualTo("expired");
    }

    @Test
    void findAll_buildsAllFilters() {
        PageResult<Coupon> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(couponUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", true, "PERCENTAGE", "X", 0, 20, "createdAt", false);
        assertThat(cap.getValue()).containsEntry("active", true).containsEntry("type", "PERCENTAGE")
                .containsEntry("search", "X");
    }

    @Test
    void findAll_nullParams_emptyFilters() {
        PageResult<Coupon> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(couponUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", null, null, null, 0, 20, "createdAt", false);
        assertThat(cap.getValue()).isEmpty();
    }

    @Test
    void findById_returnsOk() {
        Coupon coupon = Coupon.builder().id("c1").build();
        when(couponUseCase.findById("c1")).thenReturn(coupon);
        when(couponApiMapper.toDto(coupon)).thenReturn(CouponDtoOut.builder().id("c1").build());
        var resp = controller.findById("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void create_returnsCreated() {
        CouponDtoIn in = CouponDtoIn.builder().code("X").build();
        Coupon domain = Coupon.builder().code("X").build();
        Coupon created = Coupon.builder().id("id").code("X").build();
        when(couponApiMapper.toDomain(in)).thenReturn(domain);
        when(couponUseCase.create(domain)).thenReturn(created);
        when(couponApiMapper.toDto(created)).thenReturn(CouponDtoOut.builder().id("id").build());
        var resp = controller.create("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void update_returnsOk() {
        CouponDtoIn in = CouponDtoIn.builder().code("X").build();
        Coupon domain = Coupon.builder().code("X").build();
        Coupon updated = Coupon.builder().id("c1").build();
        when(couponApiMapper.toDomain(in)).thenReturn(domain);
        when(couponUseCase.update("c1", domain)).thenReturn(updated);
        when(couponApiMapper.toDto(updated)).thenReturn(CouponDtoOut.builder().build());
        var resp = controller.update("auth", "c1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void delete_returnsNoContent() {
        var resp = controller.delete("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(couponUseCase).delete("c1");
    }

    @Test
    void findUsages_mapsToDto() {
        CouponUsage usage = CouponUsage.builder().userId("u1").orderId("o1").usedAt(Instant.now()).build();
        when(couponUseCase.findUsages("c1")).thenReturn(List.of(usage));
        var resp = controller.findUsages("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void toggleActive_returnsNoContent() {
        var resp = controller.toggleActive("auth", "c1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(couponUseCase).toggleActive("c1");
    }
}
