package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import lombok.*;

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
    private Money unitPrice;
    private Money totalPrice;
}
