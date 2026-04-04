package com.backandwhite.domain.model;

import com.backandwhite.domain.valureobject.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;
    private String paymentMethod;
    private Map<String, Object> customerSnapshot;
    private List<Map<String, Object>> lines;
    private String notes;
    private String orderNumber;
    private Instant createdAt;
    private Instant updatedAt;
}
