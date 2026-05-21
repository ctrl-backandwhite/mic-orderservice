package com.backandwhite.application.service;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.exception.BusinessException;
import com.backandwhite.domain.model.Invoice;
import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Slf4j
@Service
public class InvoicePdfService {

    private final TemplateEngine templateEngine;
    private final String storeUrl;

    public InvoicePdfService(TemplateEngine templateEngine,
            @Value("${app.store.url:http://localhost:9000}") String storeUrl) {
        this.templateEngine = templateEngine;
        this.storeUrl = storeUrl;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
            Locale.of("es", "ES"));

    /**
     * Generates a PDF byte array from an Invoice domain object.
     */
    public byte[] generatePdf(Invoice invoice) {
        Context ctx = buildContext(invoice);
        String html = templateEngine.process("invoice-pdf", ctx);
        return renderHtml(invoice, html);
    }

    private Context buildContext(Invoice invoice) {
        Context ctx = new Context();
        ctx.setVariable("invoice", invoice);
        ctx.setVariable("currency", invoice.getCurrencyCode() != null ? invoice.getCurrencyCode() : "USD");

        // Status
        String statusKey = invoice.getStatus() != null ? invoice.getStatus().name().toLowerCase() : "pending";
        ctx.setVariable("statusKey", statusKey);
        ctx.setVariable("statusLabel", formatStatus(statusKey));

        // Formatted dates
        ctx.setVariable("issueDateFmt", fmtDate(invoice.getIssueDate()));
        ctx.setVariable("dueDateFmt", fmtDate(invoice.getDueDate()));

        // Customer snapshot
        applyCustomerSnapshot(ctx, invoice.getCustomerSnapshot());

        // Payment method label
        ctx.setVariable("paymentMethodLabel", formatPaymentMethod(invoice.getPaymentMethod()));

        // "Charged via" amount (when gift card or loyalty covers part of total)
        applyChargedVia(ctx, invoice);

        // QR code URL
        String verifyUrl = storeUrl + "/verificar-factura/" + invoice.getInvoiceNumber();
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data="
                + URLEncoder.encode(verifyUrl, StandardCharsets.UTF_8);
        ctx.setVariable("qrCodeUrl", qrCodeUrl);
        return ctx;
    }

    /**
     * Customer snapshot — tolerate both shapes the rest of the codebase uses:
     * either a flat {name, email, phone, address} map (built by OrderUseCaseImpl at
     * confirm time) or a granular {firstName, lastName, street, city, state,
     * zipCode, country, email, phone} map (legacy path). Missing fields resolve to
     * empty strings so the template never crashes on a partial snapshot.
     */
    private void applyCustomerSnapshot(Context ctx, Map<String, Object> customer) {
        if (customer == null) {
            ctx.setVariable("customerName", "");
            ctx.setVariable("customerEmail", "");
            ctx.setVariable("customerPhone", "");
            ctx.setVariable("customerAddress", "");
            return;
        }
        String flatName = str(customer.get("name"));
        String firstName = str(customer.get("firstName"));
        String lastName = str(customer.get("lastName"));
        String resolvedName = !flatName.isEmpty() ? flatName : (firstName + " " + lastName).trim();

        String flatAddress = str(customer.get("address"));
        String resolvedAddress = !flatAddress.isEmpty() ? flatAddress : buildAddress(customer);

        ctx.setVariable("customerName", resolvedName);
        ctx.setVariable("customerEmail", str(customer.get("email")));
        ctx.setVariable("customerPhone", str(customer.get("phone")));
        ctx.setVariable("customerAddress", resolvedAddress);
    }

    private void applyChargedVia(Context ctx, Invoice invoice) {
        Money giftCard = invoice.getGiftCardAmount() != null ? invoice.getGiftCardAmount() : Money.zero();
        Money loyalty = invoice.getLoyaltyDiscount() != null ? invoice.getLoyaltyDiscount() : Money.zero();
        if ((giftCard.isPositive() || loyalty.isPositive()) && invoice.getTotal() != null) {
            Money charged = invoice.getTotal().subtract(giftCard).subtract(loyalty);
            if (charged.isPositive()) {
                ctx.setVariable("chargedVia", charged.getAmount());
            }
        }
    }

    private byte[] renderHtml(Invoice invoice, String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (DocumentException e) {
            log.error("Error generating invoice PDF for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new BusinessException("INVOICE_PDF_GEN_FAILED", "Failed to generate invoice PDF");
        } catch (Exception e) {
            log.error("Unexpected error generating invoice PDF for invoice {}: {}", invoice.getInvoiceNumber(),
                    e.getMessage(), e);
            throw new BusinessException("INVOICE_PDF_GEN_FAILED", "Failed to generate invoice PDF");
        }
    }

    private String fmtDate(LocalDate date) {
        if (date == null)
            return null;
        return date.format(DATE_FMT);
    }

    private String formatStatus(String key) {
        return switch (key) {
            case "paid" -> "PAGADA";
            case "pending" -> "PENDIENTE";
            case "overdue" -> "VENCIDA";
            case "void" -> "ANULADA";
            default -> key.toUpperCase();
        };
    }

    private String buildAddress(Map<String, Object> customer) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, customer, "street");
        appendIfPresent(sb, customer, "city");
        appendIfPresent(sb, customer, "state");
        appendIfPresent(sb, customer, "zipCode");
        appendIfPresent(sb, customer, "country");
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, Map<String, Object> map, String key) {
        String val = str(map.get(key));
        if (!val.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(val);
        }
    }

    private String formatPaymentMethod(String method) {
        if (method == null)
            return "";
        return switch (method.toUpperCase()) {
            // Frontend sends one of {CARD, PAYPAL, USDT, BTC} (see
            // paymentMethodMap in useCheckoutSubmit.ts). Older integrations
            // send CREDIT_CARD / DEBIT_CARD — keep those for compatibility.
            case "CARD", "CREDIT_CARD" -> "Tarjeta de crédito";
            case "DEBIT_CARD" -> "Tarjeta de débito";
            case "PAYPAL" -> "PayPal";
            case "BANK_TRANSFER" -> "Transferencia bancaria";
            case "GIFT_CARD" -> "Tarjeta de regalo";
            case "MIXED" -> "Pago mixto";
            case "USDT", "CRYPTO_USDT" -> "USDT";
            case "BTC", "CRYPTO_BTC" -> "Bitcoin";
            case "NONE" -> "Sin cargo";
            default -> method.replace("_", " ");
        };
    }

    private String str(Object obj) {
        return obj == null ? "" : String.valueOf(obj).trim();
    }
}
