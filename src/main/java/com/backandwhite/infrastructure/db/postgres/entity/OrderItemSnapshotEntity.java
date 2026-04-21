package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/**
 * Immutable snapshot of a product line as the customer actually bought it: name
 * in their language, price in their currency, image URL (CJ-hosted), FX rate.
 * Means that even if CJ changes price, retires the product, or re-translates
 * the name, the order view keeps showing what the customer saw on the day they
 * paid (Fase 8.3).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_item_snapshot")
public class OrderItemSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "order_item_id", length = 64)
    private String orderItemId;

    @Column(name = "pid", length = 64)
    private String pid;

    @Column(name = "vid", length = 64)
    private String vid;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "language", length = 8)
    private String language;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "brand", length = 200)
    private String brand;

    @Column(name = "price_usd", precision = 18, scale = 4)
    private BigDecimal priceUsd;

    @Column(name = "price_customer", precision = 18, scale = 4)
    private BigDecimal priceCustomer;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "fx_rate", precision = 18, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "stock_at_purchase")
    private Integer stockAtPurchase;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
