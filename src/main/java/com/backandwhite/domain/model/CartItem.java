package com.backandwhite.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String id;
    private String cartId;
    private String productId;
    private String variantId;
    private int quantity;
    private BigDecimal unitPrice;
    private String productName;
    private String productImage;
}
