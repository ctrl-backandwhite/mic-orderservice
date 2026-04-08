package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import lombok.*;

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
    private Money unitPrice;
    private String productName;
    private String productImage;
}
