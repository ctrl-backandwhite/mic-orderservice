package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Factura")
public class InvoiceDtoOut {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "Número de factura")
    private String invoiceNumber;

    @Schema(description = "ID del pedido")
    private String orderId;

    @Schema(description = "Estado")
    private InvoiceStatus status;

    @Schema(description = "Fecha de emisión")
    private LocalDate issueDate;

    @Schema(description = "Fecha de vencimiento")
    private LocalDate dueDate;

    @Schema(description = "Subtotal")
    private BigDecimal subtotal;

    @Schema(description = "Envío")
    private BigDecimal shipping;

    @Schema(description = "Impuesto")
    private BigDecimal tax;

    @Schema(description = "Total")
    private BigDecimal total;

    @Schema(description = "Descuento por cupón")
    private BigDecimal discountAmount;

    @Schema(description = "Monto cubierto por tarjeta de regalo")
    private BigDecimal giftCardAmount;

    @Schema(description = "Descuento por puntos de lealtad")
    private BigDecimal loyaltyDiscount;

    @Schema(description = "Método de pago")
    private String paymentMethod;

    @Schema(description = "Datos del cliente")
    private Map<String, Object> customerSnapshot;

    @Schema(description = "Líneas")
    private List<Map<String, Object>> lines;

    @Schema(description = "Notas")
    private String notes;

    @Schema(description = "Número de pedido")
    private String orderNumber;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
