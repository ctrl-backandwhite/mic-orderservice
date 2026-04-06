package com.backandwhite.domain.valueobject;

import java.util.Set;

public enum OrderStatus {
    DRAFT, PENDING, CONFIRMED, PROCESSING, SHIPPED, IN_TRANSIT, DELIVERED, CANCELLED, REFUNDED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new java.util.HashMap<>() {
        {
            put(DRAFT, Set.of(PENDING, CANCELLED));
            put(PENDING, Set.of(CONFIRMED, CANCELLED));
            put(CONFIRMED, Set.of(PROCESSING, CANCELLED));
            put(PROCESSING, Set.of(SHIPPED));
            put(SHIPPED, Set.of(IN_TRANSIT));
            put(IN_TRANSIT, Set.of(DELIVERED));
            put(DELIVERED, Set.of(REFUNDED));
            put(CANCELLED, Set.of());
            put(REFUNDED, Set.of());
        }
    };

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
