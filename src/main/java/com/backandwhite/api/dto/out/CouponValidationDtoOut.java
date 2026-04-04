package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de validación de cupón")
public class CouponValidationDtoOut {

    @Schema(description = "Si el cupón es válido")
    private boolean valid;

    @Schema(description = "Descuento calculado")
    private BigDecimal discount;

    @Schema(description = "Mensaje")
    private String message;
}
