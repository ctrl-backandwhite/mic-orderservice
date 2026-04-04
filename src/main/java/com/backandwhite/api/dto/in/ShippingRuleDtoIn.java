package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear/actualizar una regla de envío")
public class ShippingRuleDtoIn {

    @NotBlank(message = "carrierId es obligatorio")
    @Schema(description = "ID del carrier", example = "car-001")
    private String carrierId;

    @NotBlank(message = "zone es obligatorio")
    @Schema(description = "Zona/país de la regla", example = "US")
    private String zone;

    @Schema(description = "Peso mínimo (kg)", example = "0")
    private BigDecimal weightMin;

    @Schema(description = "Peso máximo (kg)", example = "30")
    private BigDecimal weightMax;

    @Schema(description = "Precio mínimo del pedido", example = "0")
    private BigDecimal priceMin;

    @Schema(description = "Precio máximo del pedido", example = "9999")
    private BigDecimal priceMax;

    @NotNull(message = "rate es obligatorio")
    @Schema(description = "Tarifa de envío", example = "9.99")
    private BigDecimal rate;

    @Schema(description = "Envío gratis a partir de este monto", example = "100.00")
    private BigDecimal freeAbove;

    @Schema(description = "Días estimados de entrega", example = "5")
    private Integer estimatedDays;
}
