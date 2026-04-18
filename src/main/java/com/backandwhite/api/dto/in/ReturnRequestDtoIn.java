package com.backandwhite.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear una solicitud de devolución")
public class ReturnRequestDtoIn {

    @NotBlank(message = "orderId es obligatorio")
    @Schema(description = "ID del pedido", example = "ord-001")
    private String orderId;

    @NotBlank(message = "reason es obligatorio")
    @Schema(description = "Motivo de la devolución")
    private String reason;

    @NotNull(message = "items es obligatorio")
    @Schema(description = "Items a devolver con cantidad y motivo")
    private List<Map<String, Object>> items;

    @Schema(description = "Monto de reembolso solicitado", example = "49.99")
    private BigDecimal refundAmount;
}
