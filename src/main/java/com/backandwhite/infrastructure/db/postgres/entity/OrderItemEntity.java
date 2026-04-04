package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false, length = 64)
    private String orderId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "variant_id", length = 64)
    private String variantId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_image", length = 500)
    private String productImage;

    @Column(length = 100)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;
}
