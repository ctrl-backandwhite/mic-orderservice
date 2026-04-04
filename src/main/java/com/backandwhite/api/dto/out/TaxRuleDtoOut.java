package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valureobject.TaxType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Regla de impuesto")
public class TaxRuleDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "País")
    private String country;

    @Schema(description = "Región/estado")
    private String region;

    @Schema(description = "Tasa (%)")
    private BigDecimal rate;

    @Schema(description = "Tipo")
    private TaxType type;

    @Schema(description = "Activo")
    private boolean active;

    @Schema(description = "Categorías")
    private List<String> appliesToCategories;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
