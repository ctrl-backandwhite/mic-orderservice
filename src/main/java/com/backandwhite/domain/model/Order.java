package com.backandwhite.domain.model;

import com.backandwhite.domain.valueobject.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String couponId;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private String paymentMethod;
    private String paymentRef;
    private String notes;
    private List<OrderItem> items;
    private List<OrderStatusHistory> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
}
