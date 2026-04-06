package com.backandwhite.domain.model;

import com.backandwhite.domain.valueobject.ReturnStatus;
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
public class ReturnRequest {
    private String id;
    private String orderId;
    private String userId;
    private ReturnStatus status;
    private String reason;
    private List<Map<String, Object>> items;
    private BigDecimal refundAmount;
    private String orderNumber;
    private Instant createdAt;
    private Instant updatedAt;
}
