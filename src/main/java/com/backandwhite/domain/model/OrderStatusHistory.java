package com.backandwhite.domain.model;

import lombok.*;

import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    private String id;
    private String orderId;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private String reason;
    private Instant changedAt;
}
