package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.InvoiceDtoIn;
import com.backandwhite.api.dto.out.InvoiceDtoOut;
import com.backandwhite.api.mapper.InvoiceApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Invoice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Endpointsparagestióndefacturas")
public class InvoiceController {
    private final InvoiceUseCase invoiceUseCase;
    private final InvoiceApiMapper invoiceApiMapper;

    @GetMapping("/me")
    @Operation(summary = "Misfacturas", description = "Listalasfacturasdelusuarioautenticado")
    public ResponseEntity<PaginationDtoOut<InvoiceDtoOut>> getMyInvoices(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelusuario") @RequestHeader("X-Auth-Subject") String userId,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
        PageResult<Invoice> result = invoiceUseCase.findByUserId(userId, page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, invoiceApiMapper::toDto));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Facturaporpedido", description = "Obtienelafacturaasociadaaunpedido")
    public ResponseEntity<InvoiceDtoOut> findByOrderId(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String orderId) {
        Invoice invoice = invoiceUseCase.findByOrderId(orderId);
        return ResponseEntity.ok(invoiceApiMapper.toDto(invoice));
    }

    // ──Admin ────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "[Admin]Listarfacturas")
    public ResponseEntity<PaginationDtoOut<InvoiceDtoOut>> findAll(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "Filtrarporestado") @RequestParam(required = false) String status,
            @Parameter(description = "Filtrarporpedido") @RequestParam(required = false) String orderId,
            @Parameter(description = "Página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campodeorden") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Ascendente") @RequestParam(defaultValue = "false") boolean ascending) {
        Map<String, Object> filters = new HashMap<>();
        if (status != null)
            filters.put("status", status);
        if (orderId != null)
            filters.put("orderId", orderId);
        PageResult<Invoice> result = invoiceUseCase.findAll(filters, page, size, sortBy, ascending);
        return ResponseEntity.ok(PageableUtils.toResponse(result, invoiceApiMapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin]ObtenerfacturaporID")
    public ResponseEntity<InvoiceDtoOut> findById(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelafactura") @PathVariable String id) {
        Invoice invoice = invoiceUseCase.findById(id);
        return ResponseEntity.ok(invoiceApiMapper.toDto(invoice));
    }

    @PostMapping
    @Operation(summary = "[Admin]Crearfactura")
    public ResponseEntity<InvoiceDtoOut> create(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody InvoiceDtoIn dto) {
        Invoice invoice = invoiceApiMapper.toDomain(dto);
        Invoice created = invoiceUseCase.create(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceApiMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin]Actualizarfactura")
    public ResponseEntity<InvoiceDtoOut> update(
            @RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelafactura") @PathVariable String id,
            @Valid @RequestBody InvoiceDtoIn dto) {
        Invoice invoice = invoiceApiMapper.toDomain(dto);
        Invoice updated = invoiceUseCase.update(id, invoice);
        return ResponseEntity.ok(invoiceApiMapper.toDto(updated));
    }
}
