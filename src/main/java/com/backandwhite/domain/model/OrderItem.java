package com.backandwhite.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String id;
    private String orderId;
    private String productId;
    private String variantId;
    private String productName;
    private String productImage;
    private String sku;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
