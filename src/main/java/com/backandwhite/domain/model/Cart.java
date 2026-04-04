package com.backandwhite.domain.model;

import com.backandwhite.domain.valureobject.CartStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private String id;
    private String userId;
    private String sessionId;
    private CartStatus status;
    private Instant expiresAt;
    private List<CartItem> items;
    private BigDecimal subtotal;
    private int itemCount;
    private Instant createdAt;
    private Instant updatedAt;
}
