package com.backandwhite.api.dto.in;

import com.backandwhite.domain.valueobject.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para crear/actualizar una factura")
public class InvoiceDtoIn {

    @NotBlank(message = "orderId es obligatorio")
    @Schema(description = "ID del pedido", example = "ord-001")
    private String orderId;

    @NotNull(message = "status es obligatorio")
    @Schema(description = "Estado de la factura", example = "PENDING")
    private InvoiceStatus status;

    @Schema(description = "Fecha de emisión")
    private LocalDate issueDate;

    @Schema(description = "Fecha de vencimiento")
    private LocalDate dueDate;

    @Schema(description = "Subtotal", example = "100.00")
    private BigDecimal subtotal;

    @Schema(description = "Envío", example = "9.99")
    private BigDecimal shipping;

    @Schema(description = "Impuesto", example = "21.00")
    private BigDecimal tax;

    @Schema(description = "Total", example = "130.99")
    private BigDecimal total;

    @Schema(description = "Método de pago", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Datos del cliente")
    private Map<String, Object> customerSnapshot;

    @Schema(description = "Líneas de la factura")
    private List<Map<String, Object>> lines;

    @Schema(description = "Notas")
    private String notes;
}
