package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.ShippingCarrierDtoIn;
import com.backandwhite.api.dto.in.ShippingRuleDtoIn;
import com.backandwhite.api.dto.out.ShippingCarrierDtoOut;
import com.backandwhite.api.dto.out.ShippingOptionsDtoOut;
import com.backandwhite.api.dto.out.ShippingRuleDtoOut;
import com.backandwhite.api.mapper.ShippingTaxApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.currency.CurrencyHolder;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.ShippingCarrier;
import com.backandwhite.domain.model.ShippingRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipping")
@Tag(name = "Shipping", description = "Endpointsparagestióndeenvíos")
public class ShippingController {
    private final ShippingTaxUseCase shippingTaxUseCase;
    private final ShippingTaxApiMapper shippingTaxApiMapper;

    // ──ShippingOptions ─────────────────────────────────────────────────

    @GetMapping("/options")
    @Operation(summary = "Obteneropcionesdeenvío", description = "Devuelvelasopcionesdeenvíodisponiblesparaunpaís,pesoysubtotal")
    public ResponseEntity<ShippingOptionsDtoOut> getShippingOptions(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "País", example = "US") @RequestParam String country,
            @Parameter(description = "Peso (kg)", example = "1.5") @RequestParam(defaultValue = "1") BigDecimal weight,
            @Parameter(description = "Subtotaldelpedido", example = "99.99") @RequestParam BigDecimal subtotal) {
        List<ShippingRule> rules = shippingTaxUseCase.findShippingOptions(country, weight, Money.of(subtotal));
        List<ShippingOptionsDtoOut.ShippingOptionDto> options = rules.stream()
                .map(r -> ShippingOptionsDtoOut.ShippingOptionDto.builder()
                        .ruleId(r.getId())
                        .carrierName(r.getCarrierName())
                        .rate(r.getRate().getAmount())
                        .estimatedDays(r.getEstimatedDays())
                        .freeShipping(r.getRate().isZero())
                        .freeAbove(r.getFreeAbove() != null ? r.getFreeAbove().getAmount() : null)
                        .build())
                .toList();
        return ResponseEntity.ok(ShippingOptionsDtoOut.builder()
                .options(options)
                .currencyCode(CurrencyHolder.get())
                .build());
    }

    // ──CarriersCRUD ────────────────────────────────────────────────────

    @GetMapping("/carriers")
    @Operation(summary = "[Admin]Listarcarriers")
    public ResponseEntity<PaginationDtoOut<ShippingCarrierDtoOut>> findAllCarriers(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "true") boolean ascending) {
        PageResult<ShippingCarrier> result = shippingTaxUseCase.findAllCarriers(page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, shippingTaxApiMapper::toCarrierDto));
    }

    @GetMapping("/carriers/{id}")
    @Operation(summary = "[Admin]ObtenercarrierporID")
    public ResponseEntity<ShippingCarrierDtoOut> findCarrierById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcarrier") @PathVariable String id) {
        ShippingCarrier carrier = shippingTaxUseCase.findCarrierById(id);
        return ResponseEntity.ok(shippingTaxApiMapper.toCarrierDto(carrier));
    }

    @PostMapping("/carriers")
    @Operation(summary = "[Admin]Crearcarrier")
    public ResponseEntity<ShippingCarrierDtoOut> createCarrier(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody ShippingCarrierDtoIn dto) {
        ShippingCarrier carrier = shippingTaxApiMapper.toCarrierDomain(dto);
        ShippingCarrier created = shippingTaxUseCase.createCarrier(carrier);
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingTaxApiMapper.toCarrierDto(created));
    }

    @PutMapping("/carriers/{id}")
    @Operation(summary = "[Admin]Actualizarcarrier")
    public ResponseEntity<ShippingCarrierDtoOut> updateCarrier(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcarrier") @PathVariable String id,
            @Valid @RequestBody ShippingCarrierDtoIn dto) {
        ShippingCarrier carrier = shippingTaxApiMapper.toCarrierDomain(dto);
        ShippingCarrier updated = shippingTaxUseCase.updateCarrier(id, carrier);
        return ResponseEntity.ok(shippingTaxApiMapper.toCarrierDto(updated));
    }

    @DeleteMapping("/carriers/{id}")
    @Operation(summary = "[Admin]Eliminarcarrier")
    public ResponseEntity<Void> deleteCarrier(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelcarrier") @PathVariable String id) {
        shippingTaxUseCase.deleteCarrier(id);
        return ResponseEntity.noContent().build();
    }

    // ──RulesCRUD ───────────────────────────────────────────────────────

    @GetMapping("/rules")
    @Operation(summary = "[Admin]Listarreglasdeenvío")
    public ResponseEntity<PaginationDtoOut<ShippingRuleDtoOut>> findAllRules(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "true") boolean ascending) {
        PageResult<ShippingRule> result = shippingTaxUseCase.findAllRules(page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, shippingTaxApiMapper::toRuleDto));
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "[Admin]ObtenerregladeenvíoporID")
    public ResponseEntity<ShippingRuleDtoOut> findRuleById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id) {
        ShippingRule rule = shippingTaxUseCase.findRuleById(id);
        return ResponseEntity.ok(shippingTaxApiMapper.toRuleDto(rule));
    }

    @PostMapping("/rules")
    @Operation(summary = "[Admin]Crearregladeenvío")
    public ResponseEntity<ShippingRuleDtoOut> createRule(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody ShippingRuleDtoIn dto) {
        ShippingRule rule = shippingTaxApiMapper.toRuleDomain(dto);
        ShippingRule created = shippingTaxUseCase.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingTaxApiMapper.toRuleDto(created));
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "[Admin]Actualizarregladeenvío")
    public ResponseEntity<ShippingRuleDtoOut> updateRule(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id,
            @Valid @RequestBody ShippingRuleDtoIn dto) {
        ShippingRule rule = shippingTaxApiMapper.toRuleDomain(dto);
        ShippingRule updated = shippingTaxUseCase.updateRule(id, rule);
        return ResponseEntity.ok(shippingTaxApiMapper.toRuleDto(updated));
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "[Admin]Eliminarregladeenvío")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id) {
        shippingTaxUseCase.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
