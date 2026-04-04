package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estadísticas de pedidos")
public class OrderStatsDtoOut {

    @Schema(description = "Total de pedidos")
    private long totalOrders;

    @Schema(description = "Pedidos pendientes")
    private long pendingOrders;

    @Schema(description = "Pedidos confirmados")
    private long confirmedOrders;

    @Schema(description = "Pedidos en proceso")
    private long processingOrders;

    @Schema(description = "Pedidos enviados")
    private long shippedOrders;

    @Schema(description = "Pedidos entregados")
    private long deliveredOrders;

    @Schema(description = "Pedidos cancelados")
    private long cancelledOrders;

    @Schema(description = "Ingresos totales")
    private BigDecimal totalRevenue;

    @Schema(description = "Valor promedio de pedido")
    private BigDecimal avgOrderValue;
}
