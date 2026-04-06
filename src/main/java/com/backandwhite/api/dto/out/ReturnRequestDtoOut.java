package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.ReturnStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de devolución")
public class ReturnRequestDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "ID del pedido")
    private String orderId;

    @Schema(description = "ID del usuario")
    private String userId;

    @Schema(description = "Estado")
    private ReturnStatus status;

    @Schema(description = "Motivo")
    private String reason;

    @Schema(description = "Items a devolver")
    private List<Map<String, Object>> items;

    @Schema(description = "Monto de reembolso")
    private BigDecimal refundAmount;

    @Schema(description = "Número de pedido")
    private String orderNumber;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
