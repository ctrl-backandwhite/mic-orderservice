package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear/actualizar un carrier de envío")
public class ShippingCarrierDtoIn {

    @NotBlank(message = "name es obligatorio")
    @Schema(description = "Nombre del carrier", example = "DHL")
    private String name;

    @NotBlank(message = "code es obligatorio")
    @Schema(description = "Código del carrier", example = "DHL")
    private String code;

    @Schema(description = "URL del logo")
    private String logoUrl;

    @Schema(description = "Si está activo", example = "true")
    private boolean active;
}
