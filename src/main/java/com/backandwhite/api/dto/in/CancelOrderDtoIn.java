package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para cancelar un pedido")
public class CancelOrderDtoIn {

    @Schema(description = "Motivo de la cancelación")
    private String reason;
}
