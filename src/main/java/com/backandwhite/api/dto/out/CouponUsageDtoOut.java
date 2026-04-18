package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registro de uso de cupón")
public class CouponUsageDtoOut {

    @Schema(description = "ID del usuario")
    private String userId;

    @Schema(description = "ID del pedido")
    private String orderId;

    @Schema(description = "Fecha y hora de uso")
    private Instant usedAt;
}
