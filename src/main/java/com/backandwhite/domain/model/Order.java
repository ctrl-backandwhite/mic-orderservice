package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.valueobject.OrderSagaStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private OrderSagaStatus sagaStatus;
    private Money subtotal;
    private Money shippingCost;
    private Money taxAmount;
    private Money discountAmount;
    private Money total;
    private String couponId;
    private String giftCardCode;
    private Money giftCardAmount;
    private Integer loyaltyPointsUsed;
    private Money loyaltyDiscount;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private String paymentMethod;
    private String currencyCode;
    private BigDecimal exchangeRateToUsd;
    private String paymentRef;
    private String notes;
    private Money campaignDiscountTotal;
    private String cjOrderId;
    private String trackNumber;
    private List<OrderItem> items;
    private List<OrderStatusHistory> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
}
