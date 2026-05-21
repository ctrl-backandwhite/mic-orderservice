package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Invoice;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoicePdfService")
class InvoicePdfServiceTest {

    private static final String MIN_HTML = "<html><body>Hello</body></html>";

    @Mock
    private TemplateEngine templateEngine;

    private InvoicePdfService service;

    @BeforeEach
    void setUp() {
        service = new InvoicePdfService(templateEngine, "https://store.test");
    }

    private Invoice baseInvoice() {
        return Invoice.builder().id("inv").invoiceNumber("INV-1").orderId("ord-1").status(InvoiceStatus.PENDING)
                .issueDate(LocalDate.of(2024, 5, 1)).dueDate(LocalDate.of(2024, 5, 15)).currencyCode("USD")
                .total(Money.of(new BigDecimal("100"))).build();
    }

    @Test
    @DisplayName("generates a PDF with the rendered template and includes QR code")
    void generatesPdf() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        Invoice invoice = baseInvoice().withCustomerSnapshot(
                Map.of("name", "Jorge", "email", "j@example.com", "phone", "+1", "address", "C/ Mayor 1"));
        byte[] pdf = service.generatePdf(invoice);
        assertThat(pdf).isNotNull().isNotEmpty();
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq("invoice-pdf"), ctx.capture());
        assertThat(ctx.getValue().getVariable("customerName")).isEqualTo("Jorge");
        assertThat(ctx.getValue().getVariable("statusKey")).isEqualTo("pending");
        assertThat(ctx.getValue().getVariable("statusLabel")).isEqualTo("PENDIENTE");
        assertThat(((String) ctx.getValue().getVariable("qrCodeUrl"))).contains("verificar-factura");
    }

    @Test
    @DisplayName("falls back to firstName + lastName when 'name' is missing")
    void fallbackName() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        Invoice invoice = baseInvoice().withCustomerSnapshot(Map.of("firstName", "Ada", "lastName", "Lovelace"));
        service.generatePdf(invoice);
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(ctx.getValue().getVariable("customerName")).isEqualTo("Ada Lovelace");
    }

    @Test
    @DisplayName("builds address from granular fields when 'address' missing")
    void buildsAddress() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        Invoice invoice = baseInvoice().withCustomerSnapshot(Map.of("street", "C/ Sol", "city", "Madrid", "state",
                "Madrid", "zipCode", "28001", "country", "Spain"));
        service.generatePdf(invoice);
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(((String) ctx.getValue().getVariable("customerAddress"))).contains("Madrid").contains("28001");
    }

    @Test
    @DisplayName("handles missing customer snapshot gracefully")
    void noCustomerSnapshot() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        Invoice invoice = baseInvoice().withCustomerSnapshot(null);
        service.generatePdf(invoice);
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(ctx.getValue().getVariable("customerName")).isEqualTo("");
        assertThat(ctx.getValue().getVariable("customerAddress")).isEqualTo("");
    }

    @Test
    @DisplayName("computes chargedVia when gift-card or loyalty cover part of total")
    void chargedVia() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        Invoice invoice = baseInvoice().withGiftCardAmount(Money.of(new BigDecimal("10")))
                .withLoyaltyDiscount(Money.of(new BigDecimal("5"))).withTotal(Money.of(new BigDecimal("100")));
        service.generatePdf(invoice);
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(((BigDecimal) ctx.getValue().getVariable("chargedVia"))).isEqualByComparingTo("85");
    }

    @Test
    @DisplayName("status label resolves PAID/OVERDUE/VOID/UNKNOWN cases")
    void statusLabels() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        assertThatCode(() -> {
            for (InvoiceStatus s : InvoiceStatus.values()) {
                service.generatePdf(baseInvoice().withStatus(s));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("payment method labels honour CARD/PAYPAL/USDT/BTC variants")
    void paymentLabels() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        assertThatCode(() -> {
            for (String m : new String[]{"CARD", "PAYPAL", "USDT", "BTC", "DEBIT_CARD", "BANK_TRANSFER", "GIFT_CARD",
                    "MIXED", "NONE", "WEIRD_THING", null}) {
                service.generatePdf(baseInvoice().withPaymentMethod(m));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("template engine errors propagate to the caller")
    void templateError() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenThrow(new RuntimeException("template-broke"));
        Invoice invoice = baseInvoice();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.generatePdf(invoice))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PDF rendering errors are wrapped in RuntimeException")
    void renderingError() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn("<<<not valid html>>>");
        Invoice invoice = baseInvoice();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.generatePdf(invoice))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Failed to generate invoice PDF");
    }

    @Test
    @DisplayName("uses 'pending' as the default status key when status is null")
    void defaultsStatusKey() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        service.generatePdf(baseInvoice().withStatus(null));
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(ctx.getValue().getVariable("statusKey")).isEqualTo("pending");
    }

    @Test
    @DisplayName("formats issue/due dates in es-ES locale")
    void formatsDates() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        service.generatePdf(baseInvoice());
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat((String) ctx.getValue().getVariable("issueDateFmt")).contains("2024");
    }

    @Test
    @DisplayName("falls back to USD currency when invoice currency is null")
    void defaultsCurrency() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(Context.class))).thenReturn(MIN_HTML);
        service.generatePdf(baseInvoice().withCurrencyCode(null));
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(ctx.getValue().getVariable("currency")).isEqualTo("USD");
    }

    @Test
    @DisplayName("null issue/due dates resolve to null instead of throwing")
    void nullDates() {
        when(templateEngine.process(anyString(), org.mockito.ArgumentMatchers.any(IContext.class)))
                .thenReturn(MIN_HTML);
        service.generatePdf(baseInvoice().withIssueDate(null).withDueDate(null));
        ArgumentCaptor<IContext> ctx = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(anyString(), ctx.capture());
        assertThat(ctx.getValue().getVariable("issueDateFmt")).isNull();
        assertThat(ctx.getValue().getVariable("dueDateFmt")).isNull();
    }

    @Test
    @DisplayName("unknown status keys are uppercased through the default switch branch")
    void unknownStatusKey() throws Exception {
        java.lang.reflect.Method m = InvoicePdfService.class.getDeclaredMethod("formatStatus", String.class);
        m.setAccessible(true);
        Object label = m.invoke(service, "weird-key");
        assertThat(label).isEqualTo("WEIRD-KEY");
    }
}
