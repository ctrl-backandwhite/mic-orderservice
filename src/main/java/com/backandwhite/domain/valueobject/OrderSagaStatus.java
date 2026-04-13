package com.backandwhite.domain.valueobject;

/**
 * Tracks the Saga compensation state for an order.
 * Used in a choreography-based Saga to ensure distributed
 * consistency when payment or stock operations fail.
 */
public enum OrderSagaStatus {
    /** Order created, no Saga step started yet. */
    CREATED,
    /** Payment request published; awaiting payment service response. */
    PAYMENT_PENDING,
    /** Payment confirmed by payment service. */
    PAYMENT_CONFIRMED,
    /** Payment failed; compensation steps are being triggered. */
    PAYMENT_FAILED,
    /** Compensation in progress (stock release + failure notification). */
    COMPENSATING,
    /** All Saga steps completed successfully. */
    COMPLETED,
    /** Order and Saga fully cancelled/compensated. */
    CANCELLED
}
