package com.backandwhite.domain.model;

import java.time.Instant;
import lombok.*;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {
    private String id;
    private String orderId;
    private String status;
    private String description;
    private String location;
    private String carrier;
    private Instant eventAt;
    private Instant createdAt;
}
