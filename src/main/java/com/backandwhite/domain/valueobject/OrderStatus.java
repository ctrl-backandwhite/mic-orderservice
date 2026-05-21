package com.backandwhite.domain.valueobject;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    DRAFT, PENDING, CONFIRMED, PROCESSING, SHIPPED, IN_TRANSIT, DELIVERED, CANCELLED, REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> m = new EnumMap<>(OrderStatus.class);
        m.put(DRAFT, Set.of(PENDING, CANCELLED));
        m.put(PENDING, Set.of(CONFIRMED, CANCELLED));
        m.put(CONFIRMED, Set.of(PROCESSING, CANCELLED));
        m.put(PROCESSING, Set.of(SHIPPED));
        m.put(SHIPPED, Set.of(IN_TRANSIT));
        m.put(IN_TRANSIT, Set.of(DELIVERED));
        m.put(DELIVERED, Set.of(REFUNDED));
        m.put(CANCELLED, Set.of());
        m.put(REFUNDED, Set.of());
        TRANSITIONS = m;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
