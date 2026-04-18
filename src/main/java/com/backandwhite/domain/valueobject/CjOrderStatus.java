package com.backandwhite.domain.valueobject;

/**
 * Maps CJ Dropshipping order statuses to internal OrderStatus. Reference:
 * https://cjdropshipping.com/openapi/docs/order-status
 */
public enum CjOrderStatus {
    CREATED, IN_CART, UNPAID, IN_PRODUCTION, UNSHIPPED, SHIPPED, DELIVERED, CANCELLED,
    /** Pipeline failed at some intermediate step (will be retried by scheduler). */
    PIPELINE_FAILED,
    /** CJ balance was insufficient to complete payment; requires manual top-up. */
    AWAITING_FUNDS;

    /**
     * Maps CJ status to the closest internal {@link OrderStatus}. Returns null when
     * no mapping is applicable.
     */
    public OrderStatus toInternalStatus() {
        return switch (this) {
            case UNSHIPPED -> OrderStatus.CONFIRMED;
            case SHIPPED -> OrderStatus.SHIPPED;
            case DELIVERED -> OrderStatus.DELIVERED;
            case CANCELLED -> OrderStatus.CANCELLED;
            default -> null;
        };
    }

    public static CjOrderStatus fromString(String value) {
        if (value == null)
            return CREATED;
        try {
            return CjOrderStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CREATED;
        }
    }
}
