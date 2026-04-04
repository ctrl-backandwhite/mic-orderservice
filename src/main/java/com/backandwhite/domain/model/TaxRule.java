package com.backandwhite.domain.model;

import com.backandwhite.domain.valureobject.TaxType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRule {
    private String id;
    private String country;
    private String region;
    private BigDecimal rate;
    private TaxType type;
    private List<String> appliesToCategories;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
