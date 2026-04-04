package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.infrastructure.entity.AuditableEntity;
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
@Table(name = "shipping_rules")
public class ShippingRuleEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "carrier_id", nullable = false, length = 64)
    private String carrierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", insertable = false, updatable = false)
    private ShippingCarrierEntity carrier;

    @Column(nullable = false, length = 100)
    private String zone;

    @Column(name = "min_weight", precision = 10, scale = 2)
    private BigDecimal minWeight;

    @Column(name = "max_weight", precision = 10, scale = 2)
    private BigDecimal maxWeight;

    @Column(name = "min_price", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(name = "free_above", precision = 12, scale = 2)
    private BigDecimal freeAbove;

    @Column(name = "estimated_days", nullable = false)
    private int estimatedDays;

    @Column(nullable = false)
    private boolean active;
}
