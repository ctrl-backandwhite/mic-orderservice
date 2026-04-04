package com.backandwhite.api.dto.in;

import com.backandwhite.domain.valureobject.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para actualizar el estado de un pedido")
public class UpdateOrderStatusDtoIn {

    @NotNull(message = "status es obligatorio")
    @Schema(description = "Nuevo estado del pedido", example = "CONFIRMED")
    private OrderStatus status;

    @Schema(description = "Quién realizó el cambio", example = "admin-001")
    private String changedBy;

    @Schema(description = "Motivo del cambio")
    private String reason;
}
