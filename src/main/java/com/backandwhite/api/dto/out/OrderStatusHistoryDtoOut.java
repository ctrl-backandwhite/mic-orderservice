package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entrada de historial de estado de un pedido")
public class OrderStatusHistoryDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "Estado anterior")
    private OrderStatus fromStatus;

    @Schema(description = "Nuevo estado")
    private OrderStatus toStatus;

    @Schema(description = "Quién realizó el cambio")
    private String changedBy;

    @Schema(description = "Motivo del cambio")
    private String reason;

    @Schema(description = "Fecha del cambio")
    private Instant changedAt;
}
