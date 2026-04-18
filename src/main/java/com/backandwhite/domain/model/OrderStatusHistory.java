package com.backandwhite.domain.model;

import java.time.Instant;
import lombok.*;

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
