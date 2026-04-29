package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.domain.valueobject.MoneyConverter;
import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoices")
public class InvoiceEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money subtotal;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money shipping;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money tax;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money total;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private Money discountAmount = Money.zero();

    @Convert(converter = MoneyConverter.class)
    @Column(name = "gift_card_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private Money giftCardAmount = Money.zero();

    @Convert(converter = MoneyConverter.class)
    @Column(name = "loyalty_discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private Money loyaltyDiscount = Money.zero();

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> customerSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> lines;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
