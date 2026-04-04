package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado del cálculo de impuesto")
public class TaxCalculationDtoOut {

    @Schema(description = "Subtotal")
    private BigDecimal subtotal;

    @Schema(description = "Impuesto calculado")
    private BigDecimal taxAmount;

    @Schema(description = "Total con impuesto")
    private BigDecimal totalWithTax;
}
