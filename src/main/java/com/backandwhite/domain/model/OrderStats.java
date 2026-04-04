package com.backandwhite.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {
    private long totalOrders;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal avgOrderValue;
}
