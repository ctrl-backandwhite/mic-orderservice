package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.CouponDtoIn;
import com.backandwhite.api.dto.in.ValidateCouponDtoIn;
import com.backandwhite.api.dto.out.CouponDtoOut;
import com.backandwhite.api.dto.out.CouponValidationDtoOut;
import com.backandwhite.api.mapper.CouponApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Coupon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Endpointsparagestióndecupones")
public class CouponController {
    private final CouponUseCase couponUseCase;
    private final CouponApiMapper couponApiMapper;

    @PostMapping("/validate")
    @Operation(summary = "Validarcupón", description = "Validauncupónydevuelveeldescuentocalculado")
    public ResponseEntity<CouponValidationDtoOut> validate(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader(value = "X-Auth-Subject", required = false) String userId,
            @Valid @RequestBody ValidateCouponDtoIn dto) {
        try {
            BigDecimal discount = couponUseCase.validate(dto.getCode(), dto.getCartSubtotal(), userId);
            return ResponseEntity.ok(CouponValidationDtoOut.builder()
                    .valid(true)
                    .discount(discount)
                    .message("Cupónválido")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(CouponValidationDtoOut.builder()
                    .valid(false)
                    .discount(BigDecimal.ZERO)
                    .message(e.getMessage())
                    .build());
        }
    }

    // ──AdminCRUD ───────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "[Admin]Listarcupones")
    public ResponseEntity<PaginationDtoOut<CouponDtoOut>> findAll(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Filtrarporactivo") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filtrarportipo") @RequestParam(required = false) String type,
            @Parameter(description = "Buscarporcódigo") @RequestParam(required = false) String search,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
        Map<String, Object> filters = new HashMap<>();
        if (active != null)
            filters.put("active", active);
        if (type != null)
            filters.put("type", type);
        if (search != null)
            filters.put("search", search);
        PageResult<Coupon> result = couponUseCase.findAll(filters, page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, couponApiMapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin]ObtenercupónporID")
    public ResponseEntity<CouponDtoOut> findById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcupón") @PathVariable String id) {
        Coupon coupon = couponUseCase.findById(id);
        return ResponseEntity.ok(couponApiMapper.toDto(coupon));
    }

    @PostMapping
    @Operation(summary = "[Admin]Crearcupón")
    public ResponseEntity<CouponDtoOut> create(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody CouponDtoIn dto) {
        Coupon coupon = couponApiMapper.toDomain(dto);
        Coupon created = couponUseCase.create(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponApiMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin]Actualizarcupón")
    public ResponseEntity<CouponDtoOut> update(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcupón") @PathVariable String id,
            @Valid @RequestBody CouponDtoIn dto) {
        Coupon coupon = couponApiMapper.toDomain(dto);
        Coupon updated = couponUseCase.update(id, coupon);
        return ResponseEntity.ok(couponApiMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin]Eliminarcupón")
    public ResponseEntity<Void> delete(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcupón") @PathVariable String id) {
        couponUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "[Admin]Activar/desactivarcupón")
    public ResponseEntity<Void> toggleActive(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcupón") @PathVariable String id) {
        couponUseCase.toggleActive(id);
        return ResponseEntity.noContent().build();
    }
}
