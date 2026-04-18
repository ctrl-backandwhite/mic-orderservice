package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Carrier de envío")
public class ShippingCarrierDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "Nombre")
    private String name;

    @Schema(description = "Código")
    private String code;

    @Schema(description = "URL del logo")
    private String logoUrl;

    @Schema(description = "Activo")
    private boolean active;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
