package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.InvoiceDtoIn;
import com.backandwhite.api.dto.out.InvoiceDtoOut;
import com.backandwhite.api.mapper.InvoiceApiMapper;
import com.backandwhite.application.service.InvoicePdfService;
import com.backandwhite.application.service.InvoicePdfUrlSigner;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.Invoice;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceUseCase invoiceUseCase;
    @Mock
    private InvoiceApiMapper invoiceApiMapper;
    @Mock
    private InvoicePdfService invoicePdfService;
    @Mock
    private InvoicePdfUrlSigner invoicePdfUrlSigner;

    @InjectMocks
    private InvoiceController controller;

    @Test
    void getMyInvoices_returnsOk() {
        PageResult<Invoice> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        when(invoiceUseCase.findByUserId("u1", 0, 20, "createdAt", false)).thenReturn(pr);
        var resp = controller.getMyInvoices("auth", "u1", 0, 20, "createdAt", false);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void findByOrderId_returnsOk() {
        Invoice inv = Invoice.builder().id("i1").build();
        when(invoiceUseCase.findByOrderId("o1")).thenReturn(inv);
        when(invoiceApiMapper.toDto(inv)).thenReturn(InvoiceDtoOut.builder().id("i1").build());
        var resp = controller.findByOrderId("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void downloadInvoicePdf_returnsPdf() {
        Invoice inv = Invoice.builder().id("i1").invoiceNumber("INV-1").build();
        byte[] pdf = new byte[]{1, 2, 3};
        when(invoiceUseCase.findByOrderId("o1")).thenReturn(inv);
        when(invoicePdfService.generatePdf(inv)).thenReturn(pdf);
        var resp = controller.downloadInvoicePdf("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(pdf);
        assertThat(resp.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(resp.getHeaders().getContentLength()).isEqualTo(3);
    }

    @Test
    void downloadInvoicePdfPublic_invalidSignature_returns404() {
        when(invoicePdfUrlSigner.verify("o1", 12345L, "bad")).thenReturn(false);
        var resp = controller.downloadInvoicePdfPublic("o1", 12345L, "bad");
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void downloadInvoicePdfPublic_validSignature_returnsPdf() {
        Invoice inv = Invoice.builder().id("i1").invoiceNumber("INV-1").build();
        byte[] pdf = new byte[]{9};
        when(invoicePdfUrlSigner.verify("o1", 12345L, "ok")).thenReturn(true);
        when(invoiceUseCase.findByOrderId("o1")).thenReturn(inv);
        when(invoicePdfService.generatePdf(inv)).thenReturn(pdf);
        var resp = controller.downloadInvoicePdfPublic("o1", 12345L, "ok");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualTo(pdf);
    }

    @Test
    void findAll_buildsFilters() {
        PageResult<Invoice> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(invoiceUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", "PAID", "o1", 0, 20, "createdAt", false);
        assertThat(cap.getValue()).containsEntry("status", "PAID").containsEntry("orderId", "o1");
    }

    @Test
    void findAll_nullParams_emptyFilters() {
        PageResult<Invoice> pr = new PageResult<>(List.of(), 0, 0, 0, 20, false, false);
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        when(invoiceUseCase.findAll(cap.capture(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(pr);
        controller.findAll("auth", null, null, 0, 20, "createdAt", false);
        assertThat(cap.getValue()).isEmpty();
    }

    @Test
    void findById_admin_returnsOk() {
        Invoice inv = Invoice.builder().id("i1").build();
        when(invoiceUseCase.findById("i1")).thenReturn(inv);
        when(invoiceApiMapper.toDto(inv)).thenReturn(InvoiceDtoOut.builder().build());
        var resp = controller.findById("auth", "i1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void create_returnsCreated() {
        InvoiceDtoIn in = InvoiceDtoIn.builder().build();
        Invoice domain = Invoice.builder().build();
        Invoice created = Invoice.builder().id("i1").build();
        when(invoiceApiMapper.toDomain(in)).thenReturn(domain);
        when(invoiceUseCase.create(domain)).thenReturn(created);
        when(invoiceApiMapper.toDto(created)).thenReturn(InvoiceDtoOut.builder().build());
        var resp = controller.create("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void update_returnsOk() {
        InvoiceDtoIn in = InvoiceDtoIn.builder().build();
        Invoice domain = Invoice.builder().build();
        Invoice updated = Invoice.builder().id("i1").build();
        when(invoiceApiMapper.toDomain(in)).thenReturn(domain);
        when(invoiceUseCase.update("i1", domain)).thenReturn(updated);
        when(invoiceApiMapper.toDto(updated)).thenReturn(InvoiceDtoOut.builder().build());
        var resp = controller.update("auth", "i1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
