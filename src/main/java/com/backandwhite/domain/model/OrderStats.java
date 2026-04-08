package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import lombok.*;

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
    private Money totalRevenue;
    private Money avgOrderValue;
}
