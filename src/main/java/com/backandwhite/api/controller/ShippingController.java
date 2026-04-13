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
import com.backandwhite.common.currency.CurrencyRateCache;
import com.backandwhite.common.currency.PriceConversionService;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.domain.model.CjFreightOption;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipping")
@Tag(name = "Shipping", description = "Endpointsparagestióndeenvíos")
public class ShippingController {
    private final ShippingTaxUseCase shippingTaxUseCase;
    private final ShippingTaxApiMapper shippingTaxApiMapper;
    private final CurrencyRateCache currencyRateCache;
    private final PriceConversionService priceConversionService;
    private final CjShoppingPort cjShoppingPort;

    /** Default shipping rate in USD when no rules are defined for the country */
    private static final BigDecimal DEFAULT_RATE_USD = new BigDecimal("5.00");
    private static final int DEFAULT_ESTIMATED_DAYS = 7;
    private static final String DEFAULT_CARRIER_NAME = "Envío estándar";
    private static final String DEFAULT_RULE_ID = "DEFAULT";

    // ──ShippingOptions ─────────────────────────────────────────────────

    @GetMapping("/options")
    @Operation(summary = "Obteneropcionesdeenvío", description = "Devuelvelasopcionesdeenvíodisponiblesparaunpaís,pesoysubtotal")
    public ResponseEntity<ShippingOptionsDtoOut> getShippingOptions(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "País", example = "US") @RequestParam String country,
            @Parameter(description = "Peso (kg)", example = "1.5") @RequestParam(defaultValue = "1") BigDecimal weight,
            @Parameter(description = "Subtotaldelpedido", example = "99.99") @RequestParam BigDecimal subtotal) {

        // Frontend sends subtotal in display currency; convert back to USD
        // so the freeAbove comparison (stored in USD) works correctly.
        String targetCurrency = CurrencyHolder.get();
        BigDecimal rate = currencyRateCache.getRate(targetCurrency);
        BigDecimal subtotalUsd = (rate.compareTo(BigDecimal.ZERO) > 0)
                ? subtotal.divide(rate, 2, RoundingMode.HALF_UP)
                : subtotal;

        List<ShippingRule> rules = shippingTaxUseCase.findShippingOptions(country, weight, Money.of(subtotalUsd));

        List<ShippingOptionsDtoOut.ShippingOptionDto> options;

        if (rules.isEmpty()) {
            // No rules for this country → return a single default option at $5 USD.
            // Frontend converts to display currency via convertFromUsd().
            options = List.of(ShippingOptionsDtoOut.ShippingOptionDto.builder()
                    .ruleId(DEFAULT_RULE_ID)
                    .carrierName(DEFAULT_CARRIER_NAME)
                    .rate(DEFAULT_RATE_USD)
                    .estimatedDays(DEFAULT_ESTIMATED_DAYS)
                    .freeShipping(false)
                    .freeAbove(null)
                    .build());
        } else {
            // Return raw USD amounts; frontend handles display-currency conversion.
            options = rules.stream()
                    .map(r -> ShippingOptionsDtoOut.ShippingOptionDto.builder()
                            .ruleId(r.getId())
                            .carrierName(r.getCarrierName())
                            .rate(r.getRate().isZero() ? BigDecimal.ZERO : r.getRate().getAmount())
                            .estimatedDays(r.getEstimatedDays())
                            .freeShipping(r.getRate().isZero())
                            .freeAbove(r.getFreeAbove() != null ? r.getFreeAbove().getAmount() : null)
                            .build())
                    .toList();
        }

        return ResponseEntity.ok(ShippingOptionsDtoOut.builder()
                .options(options)
                .currencyCode("USD")
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

    // ── CJ Freight Calculation ─────────────────────────────────────────────

    @GetMapping("/freight/calculate")
    @Operation(summary = "Calculate CJ freight options", description = "Queries CJ Dropshipping for available logistics options. "
            +
            "Pass toCountry, vid (CJ variant ID), and qty as query parameters.")
    public ResponseEntity<List<CjFreightOption>> calculateFreight(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Destination country ISO-2 code, e.g. US") @RequestParam String toCountry,
            @Parameter(description = "CJ variant ID") @RequestParam String vid,
            @Parameter(description = "Quantity") @RequestParam(defaultValue = "1") int qty) {

        CjFreightOption.ProductItem item = new CjFreightOption.ProductItem(vid, qty);
        List<CjFreightOption> options = cjShoppingPort.calculateFreight(toCountry, List.of(item));
        return ResponseEntity.ok(options);
    }
}
