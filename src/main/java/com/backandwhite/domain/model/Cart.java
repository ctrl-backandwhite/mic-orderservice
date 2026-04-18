package com.backandwhite.domain.model;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.valueobject.CartStatus;
import java.time.Instant;
import java.util.List;
import lombok.*;

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
    private Money subtotal;
    private int itemCount;
    private Instant createdAt;
    private Instant updatedAt;
}
