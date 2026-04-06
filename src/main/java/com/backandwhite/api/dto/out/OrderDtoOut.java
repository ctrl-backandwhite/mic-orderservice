package com.backandwhite.api.dto.out;

import com.backandwhite.domain.valueobject.OrderStatus;
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
@Schema(description = "Pedido")
public class OrderDtoOut {

    @Schema(description = "ID del pedido")
    private String id;

    @Schema(description = "Número de pedido", example = "NX-20250101-12345")
    private String orderNumber;

    @Schema(description = "ID del usuario")
    private String userId;

    @Schema(description = "Estado del pedido")
    private OrderStatus status;

    @Schema(description = "Subtotal")
    private BigDecimal subtotal;

    @Schema(description = "Costo de envío")
    private BigDecimal shippingCost;

    @Schema(description = "Impuestos")
    private BigDecimal taxAmount;

    @Schema(description = "Descuento")
    private BigDecimal discountAmount;

    @Schema(description = "Total")
    private BigDecimal total;

    @Schema(description = "ID del cupón aplicado")
    private String couponId;

    @Schema(description = "Dirección de envío")
    private Map<String, Object> shippingAddress;

    @Schema(description = "Dirección de facturación")
    private Map<String, Object> billingAddress;

    @Schema(description = "Método de pago")
    private String paymentMethod;

    @Schema(description = "Referencia de pago")
    private String paymentRef;

    @Schema(description = "Notas")
    private String notes;

    @Schema(description = "Items del pedido")
    private List<OrderItemDtoOut> items;

    @Schema(description = "Historial de estados")
    private List<OrderStatusHistoryDtoOut> statusHistory;

    @Schema(description = "Fecha de creación")
    private Instant createdAt;

    @Schema(description = "Fecha de actualización")
    private Instant updatedAt;
}
