package com.backandwhite.api.controller;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.dto.in.InvoiceDtoIn;
import com.backandwhite.api.dto.out.InvoiceDtoOut;
import com.backandwhite.api.mapper.InvoiceApiMapper;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.application.service.InvoicePdfService;
import com.backandwhite.application.service.InvoicePdfUrlSigner;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.common.security.annotation.NxAdmin;
import com.backandwhite.common.security.annotation.NxPublic;
import com.backandwhite.common.security.annotation.NxUser;
import com.backandwhite.domain.model.Invoice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Endpointsparagestióndefacturas")
public class InvoiceController {
    private final InvoiceUseCase invoiceUseCase;
    private final InvoiceApiMapper invoiceApiMapper;
    private final InvoicePdfService invoicePdfService;
    private final InvoicePdfUrlSigner invoicePdfUrlSigner;

    @NxUser
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

    @NxUser
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Facturaporpedido", description = "Obtienelafacturaasociadaaunpedido")
    public ResponseEntity<InvoiceDtoOut> findByOrderId(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelpedido") @PathVariable String orderId) {
        Invoice invoice = invoiceUseCase.findByOrderId(orderId);
        return ResponseEntity.ok(invoiceApiMapper.toDto(invoice));
    }

    @NxUser
    @GetMapping("/order/{orderId}/pdf")
    @Operation(summary = "Descargar factura PDF", description = "Genera y descarga la factura en formato PDF")
    public ResponseEntity<byte[]> downloadInvoicePdf(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "ID del pedido") @PathVariable String orderId) {
        Invoice invoice = invoiceUseCase.findByOrderId(orderId);
        byte[] pdf = invoicePdfService.generatePdf(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", invoice.getInvoiceNumber() + ".pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Public signed-URL endpoint: lets the customer download the invoice from the
     * email link without authenticating. The URL carries an HMAC signature that
     * covers {@code orderId + exp}; invalid or expired links return 404 to stay
     * invisible to probes.
     */
    @NxPublic
    @GetMapping("/public/order/{orderId}/pdf")
    @Operation(summary = "Descargar factura PDF (enlace firmado del email)")
    public ResponseEntity<byte[]> downloadInvoicePdfPublic(@PathVariable String orderId, @RequestParam long exp,
            @RequestParam String sig) {
        if (!invoicePdfUrlSigner.verify(orderId, exp, sig)) {
            return ResponseEntity.notFound().build();
        }
        Invoice invoice = invoiceUseCase.findByOrderId(orderId);
        byte[] pdf = invoicePdfService.generatePdf(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", invoice.getInvoiceNumber() + ".pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ──Admin ────────────────────────────────────────────────────────────

    @NxAdmin
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

    @NxAdmin
    @GetMapping("/{id}")
    @Operation(summary = "[Admin]ObtenerfacturaporID")
    public ResponseEntity<InvoiceDtoOut> findById(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelafactura") @PathVariable String id) {
        Invoice invoice = invoiceUseCase.findById(id);
        return ResponseEntity.ok(invoiceApiMapper.toDto(invoice));
    }

    @NxAdmin
    @PostMapping
    @Operation(summary = "[Admin]Crearfactura")
    public ResponseEntity<InvoiceDtoOut> create(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Valid @RequestBody InvoiceDtoIn dto) {
        Invoice invoice = invoiceApiMapper.toDomain(dto);
        Invoice created = invoiceUseCase.create(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceApiMapper.toDto(created));
    }

    @NxAdmin
    @PutMapping("/{id}")
    @Operation(summary = "[Admin]Actualizarfactura")
    public ResponseEntity<InvoiceDtoOut> update(@RequestHeader(AppConstants.HEADER_NX036_AUTH) String nxAuth,
            @Parameter(description = "IDdelafactura") @PathVariable String id, @Valid @RequestBody InvoiceDtoIn dto) {
        Invoice invoice = invoiceApiMapper.toDomain(dto);
        Invoice updated = invoiceUseCase.update(id, invoice);
        return ResponseEntity.ok(invoiceApiMapper.toDto(updated));
    }
}
