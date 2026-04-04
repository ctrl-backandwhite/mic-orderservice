package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import com.backandwhite.domain.valureobject.TaxType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tax_rules")
public class TaxRuleEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 10)
    private String country;

    @Column(length = 100)
    private String region;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applies_to_categories", columnDefinition = "jsonb")
    private List<String> appliesToCategories;

    @Column(nullable = false)
    private boolean active;
}
