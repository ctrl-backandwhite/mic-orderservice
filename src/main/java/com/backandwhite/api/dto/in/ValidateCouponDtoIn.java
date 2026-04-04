package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para validar un cupón")
public class ValidateCouponDtoIn {

    @NotBlank(message = "code es obligatorio")
    @Schema(description = "Código del cupón", example = "WELCOME10")
    private String code;

    @Schema(description = "Subtotal del carrito", example = "100.00")
    private BigDecimal cartSubtotal;
}
