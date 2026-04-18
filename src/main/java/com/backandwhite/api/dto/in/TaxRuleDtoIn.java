package com.backandwhite.api.dto.in;

import com.backandwhite.domain.valueobject.TaxType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear/actualizar una regla de impuesto")
public class TaxRuleDtoIn {

    @NotBlank(message = "country es obligatorio")
    @Schema(description = "País", example = "US")
    private String country;

    @Schema(description = "Región/estado", example = "CA")
    private String region;

    @NotNull(message = "rate es obligatorio")
    @Schema(description = "Tasa de impuesto (%)", example = "21.00")
    private BigDecimal rate;

    @NotNull(message = "type es obligatorio")
    @Schema(description = "Tipo de impuesto", example = "VAT")
    private TaxType type;

    @Schema(description = "Si está activa", example = "true")
    private boolean active;

    @Schema(description = "Categorías a las que aplica (null = todas)")
    private List<String> appliesToCategories;
}
