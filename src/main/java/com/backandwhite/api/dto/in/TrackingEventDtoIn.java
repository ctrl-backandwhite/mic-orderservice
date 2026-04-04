package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para agregar un evento de tracking")
public class TrackingEventDtoIn {

    @NotBlank(message = "orderId es obligatorio")
    @Schema(description = "ID del pedido", example = "ord-001")
    private String orderId;

    @NotBlank(message = "status es obligatorio")
    @Schema(description = "Estado del tracking", example = "IN_TRANSIT")
    private String status;

    @Schema(description = "Descripción del evento")
    private String description;

    @Schema(description = "Ubicación", example = "Madrid, Spain")
    private String location;

    @Schema(description = "Carrier", example = "DHL")
    private String carrier;

    @NotNull(message = "eventAt es obligatorio")
    @Schema(description = "Fecha del evento")
    private Instant eventAt;
}
