package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para actualizar la cantidad de un item")
public class CartItemQuantityDtoIn {

    @NotNull(message = "quantity es obligatorio")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Schema(description = "Nueva cantidad", example = "3")
    private Integer quantity;
}
