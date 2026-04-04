package com.backandwhite.domain.valureobject;

import java.util.Set;

public enum OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, IN_TRANSIT, DELIVERED, CANCELLED, REFUNDED;

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = java.util.Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PROCESSING, CANCELLED),
            PROCESSING, Set.of(SHIPPED),
            SHIPPED, Set.of(IN_TRANSIT),
            IN_TRANSIT, Set.of(DELIVERED),
            DELIVERED, Set.of(REFUNDED),
            CANCELLED, Set.of(),
            REFUNDED, Set.of());

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
