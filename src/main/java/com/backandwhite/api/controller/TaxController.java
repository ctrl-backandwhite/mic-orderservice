package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.TaxRuleDtoIn;
import com.backandwhite.api.dto.out.TaxCalculationDtoOut;
import com.backandwhite.api.dto.out.TaxRuleDtoOut;
import com.backandwhite.api.mapper.ShippingTaxApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.domain.model.TaxRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/taxes")
@Tag(name = "Taxes", description = "Endpointsparagestióndeimpuestos")
public class TaxController {
    private final ShippingTaxUseCase shippingTaxUseCase;
    private final ShippingTaxApiMapper shippingTaxApiMapper;

    @GetMapping("/calculate")
    @Operation(summary = "Calcularimpuesto", description = "Calculaelimpuestoparaunpaís,regiónysubtotal")
    public ResponseEntity<TaxCalculationDtoOut> calculateTax(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "País", example = "US") @RequestParam String country,
            @Parameter(description = "Región/estado", example = "CA") @RequestParam(required = false) String region,
            @Parameter(description = "Subtotal", example = "99.99") @RequestParam BigDecimal subtotal) {
        BigDecimal taxAmount = shippingTaxUseCase.calculateTax(country, region, subtotal);
        return ResponseEntity.ok(TaxCalculationDtoOut.builder()
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalWithTax(subtotal.add(taxAmount))
                .build());
    }

    @GetMapping
    @Operation(summary = "[Admin]Listarreglasdeimpuesto")
    public ResponseEntity<PaginationDtoOut<TaxRuleDtoOut>> findAll(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "true") boolean ascending) {
        PageResult<TaxRule> result = shippingTaxUseCase.findAllTaxRules(page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, shippingTaxApiMapper::toTaxRuleDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin]ObtenerregladeimpuestoporID")
    public ResponseEntity<TaxRuleDtoOut> findById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id) {
        TaxRule rule = shippingTaxUseCase.findTaxRuleById(id);
        return ResponseEntity.ok(shippingTaxApiMapper.toTaxRuleDto(rule));
    }

    @PostMapping
    @Operation(summary = "[Admin]Crearregladeimpuesto")
    public ResponseEntity<TaxRuleDtoOut> create(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody TaxRuleDtoIn dto) {
        TaxRule rule = shippingTaxApiMapper.toTaxRuleDomain(dto);
        TaxRule created = shippingTaxUseCase.createTaxRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingTaxApiMapper.toTaxRuleDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin]Actualizarregladeimpuesto")
    public ResponseEntity<TaxRuleDtoOut> update(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id,
            @Valid @RequestBody TaxRuleDtoIn dto) {
        TaxRule rule = shippingTaxApiMapper.toTaxRuleDomain(dto);
        TaxRule updated = shippingTaxUseCase.updateTaxRule(id, rule);
        return ResponseEntity.ok(shippingTaxApiMapper.toTaxRuleDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin]Eliminarregladeimpuesto")
    public ResponseEntity<Void> delete(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelaregla") @PathVariable String id) {
        shippingTaxUseCase.deleteTaxRule(id);
        return ResponseEntity.noContent().build();
    }
}
