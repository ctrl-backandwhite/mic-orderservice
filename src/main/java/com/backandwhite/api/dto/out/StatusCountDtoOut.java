package com.backandwhite.api.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Count of orders for a single status value. Returned by
 * {@code /api/v1/orders/stats/status-distribution}.
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order count bucketed by status")
public class StatusCountDtoOut {

    @Schema(description = "Order status (PENDING, CONFIRMED, PROCESSING, SHIPPED, IN_TRANSIT, DELIVERED, CANCELLED, REFUNDED)")
    private String status;

    @Schema(description = "Number of orders in this status within the requested window")
    private long count;
}
