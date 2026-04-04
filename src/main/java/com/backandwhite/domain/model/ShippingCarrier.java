package com.backandwhite.domain.model;

import lombok.*;

import java.time.Instant;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCarrier {
    private String id;
    private String name;
    private String code;
    private String logoUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
