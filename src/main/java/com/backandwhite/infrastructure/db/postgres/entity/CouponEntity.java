package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.domain.valueobject.MoneyConverter;
import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import com.backandwhite.domain.valueobject.CouponType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupons")
public class CouponEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money value;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private Money minOrderAmount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applies_to_categories", columnDefinition = "jsonb")
    private List<String> appliesToCategories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applies_to_products", columnDefinition = "jsonb")
    private List<String> appliesToProducts;

    @Column(nullable = false)
    private boolean active;
}
