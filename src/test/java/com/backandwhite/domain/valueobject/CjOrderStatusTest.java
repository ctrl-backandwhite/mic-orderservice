package com.backandwhite.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CjOrderStatus")
class CjOrderStatusTest {

    @Test
    @DisplayName("fromString returns CREATED for null input")
    void fromNull() {
        assertThat(CjOrderStatus.fromString(null)).isEqualTo(CjOrderStatus.CREATED);
    }

    @Test
    @DisplayName("fromString uppercases and parses recognised values")
    void fromValid() {
        assertThat(CjOrderStatus.fromString("shipped")).isEqualTo(CjOrderStatus.SHIPPED);
        assertThat(CjOrderStatus.fromString("DELIVERED")).isEqualTo(CjOrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("fromString falls back to CREATED on unknown values")
    void fromUnknown() {
        assertThat(CjOrderStatus.fromString("MYSTERY")).isEqualTo(CjOrderStatus.CREATED);
        assertThat(CjOrderStatus.fromString("")).isEqualTo(CjOrderStatus.CREATED);
    }

    @Test
    @DisplayName("toInternalStatus maps the four canonical statuses to OrderStatus")
    void mapsKnownInternalStatuses() {
        assertThat(CjOrderStatus.UNSHIPPED.toInternalStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(CjOrderStatus.SHIPPED.toInternalStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(CjOrderStatus.DELIVERED.toInternalStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(CjOrderStatus.CANCELLED.toInternalStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("toInternalStatus returns null for non-mapped CJ-only statuses")
    void mapsUnmappedReturnsNull() {
        assertThat(CjOrderStatus.PIPELINE_FAILED.toInternalStatus()).isNull();
        assertThat(CjOrderStatus.AWAITING_FUNDS.toInternalStatus()).isNull();
        assertThat(CjOrderStatus.CREATED.toInternalStatus()).isNull();
        assertThat(CjOrderStatus.IN_CART.toInternalStatus()).isNull();
        assertThat(CjOrderStatus.UNPAID.toInternalStatus()).isNull();
        assertThat(CjOrderStatus.IN_PRODUCTION.toInternalStatus()).isNull();
    }
}
