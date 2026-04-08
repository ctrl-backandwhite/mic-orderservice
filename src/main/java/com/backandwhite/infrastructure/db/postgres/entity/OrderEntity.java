package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.domain.valueobject.MoneyConverter;
import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import com.backandwhite.domain.valueobject.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class OrderEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private OrderStatus status;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money subtotal;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private Money shippingCost;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private Money taxAmount;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private Money discountAmount;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money total;

    @Column(name = "coupon_id", length = 64)
    private String couponId;

    @Column(name = "gift_card_code", length = 50)
    private String giftCardCode;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "gift_card_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private Money giftCardAmount = Money.zero();

    @Column(name = "loyalty_points_used", nullable = false)
    @Builder.Default
    private Integer loyaltyPointsUsed = 0;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "loyalty_discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private Money loyaltyDiscount = Money.zero();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> shippingAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Map<String, Object> billingAddress;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "exchange_rate_to_usd", nullable = false, precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE;

    @Column(name = "payment_ref", length = 255)
    private String paymentRef;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<OrderStatusHistoryEntity> statusHistory = new ArrayList<>();
}
