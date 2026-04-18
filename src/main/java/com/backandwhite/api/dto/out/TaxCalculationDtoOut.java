package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.*;

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

    @Schema(description = "Código ISO 4217 de la moneda", example = "EUR")
    private String currencyCode;
}
