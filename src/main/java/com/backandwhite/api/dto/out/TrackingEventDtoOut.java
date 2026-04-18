package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evento de tracking")
public class TrackingEventDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "ID del pedido")
    private String orderId;

    @Schema(description = "Estado")
    private String status;

    @Schema(description = "Descripción")
    private String description;

    @Schema(description = "Ubicación")
    private String location;

    @Schema(description = "Carrier")
    private String carrier;

    @Schema(description = "Fecha del evento")
    private Instant eventAt;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;
}
