package com.backandwhite.domain.model;

import com.backandwhite.domain.valueobject.OrderStatus;
import lombok.*;

/**
 * Domain model pairing an order status with its count inside a time window.
 * Returned by {@code OrderRepository.findStatusDistribution}.
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusCount {
    private OrderStatus status;
    private long count;
}
