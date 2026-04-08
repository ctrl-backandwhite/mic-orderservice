package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Regla de envío")
public class ShippingRuleDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "ID del carrier")
    private String carrierId;

    @Schema(description = "Zona")
    private String zone;

    @Schema(description = "Peso mínimo")
    private BigDecimal weightMin;

    @Schema(description = "Peso máximo")
    private BigDecimal weightMax;

    @Schema(description = "Precio mínimo")
    private BigDecimal priceMin;

    @Schema(description = "Precio máximo")
    private BigDecimal priceMax;

    @Schema(description = "Tarifa")
    private BigDecimal rate;

    @Schema(description = "Envío gratis a partir de")
    private BigDecimal freeAbove;

    @Schema(description = "Días estimados")
    private Integer estimatedDays;

    @Schema(description = "Nombre del carrier")
    private String carrierName;

    @Schema(description = "Activo")
    private boolean active;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
