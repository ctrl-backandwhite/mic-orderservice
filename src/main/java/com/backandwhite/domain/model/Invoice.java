package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private String id;
    private String invoiceNumber;
    private String orderId;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Money subtotal;
    private Money shipping;
    private Money tax;
    private Money total;
    private Money discountAmount;
    private Money giftCardAmount;
    private Money loyaltyDiscount;
    private String paymentMethod;
    private String currencyCode;
    private Map<String, Object> customerSnapshot;
    private List<Map<String, Object>> lines;
    private String notes;
    private String orderNumber;
    private Instant createdAt;
    private Instant updatedAt;
}
