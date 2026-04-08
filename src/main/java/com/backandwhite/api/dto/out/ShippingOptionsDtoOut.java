package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Opciones de envío disponibles")
public class ShippingOptionsDtoOut {

    @Schema(description = "Opciones disponibles")
    private List<ShippingOptionDto> options;

    @Data
    @With
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Opción de envío")
    public static class ShippingOptionDto {

        @Schema(description = "ID de la regla")
        private String ruleId;

        @Schema(description = "Nombre del carrier")
        private String carrierName;

        @Schema(description = "Tarifa")
        private BigDecimal rate;

        @Schema(description = "Días estimados")
        private Integer estimatedDays;

        @Schema(description = "Es envío gratuito")
        private boolean freeShipping;

        @Schema(description = "Umbral para envío gratuito")
        private BigDecimal freeAbove;
    }
}
