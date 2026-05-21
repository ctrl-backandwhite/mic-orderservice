package com.backandwhite.infrastructure.message.kafka.producer;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NoOpOrderEventAdapterTest {

    private final NoOpOrderEventAdapter adapter = new NoOpOrderEventAdapter();

    @Test
    void publishOrderCreated_doesNotThrow() {
        assertThatCode(
                () -> adapter.publishOrderCreated("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "DRAFT", 2, "addr-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderConfirmed_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderConfirmed("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "100.00", 2))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderStatusUpdated_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderStatusUpdated("o1", "u1", "e@e.com", "NX-1", "DRAFT", "PENDING"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderCancelled_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderCancelled("o1", "u1", "e@e.com", "NX-1", "user-cancel"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderShipped_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderShipped("o1", "u1", "e@e.com", "NX-1", "TRACK-1", "DHL", "2026-01-01"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderDelivered_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderDelivered("o1", "u1", "e@e.com", "NX-1", "100.00"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderReturnRequested_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderReturnRequested("o1", "rr1", "u1", "e@e.com", "NX-1", "defective"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishOrderReturnApproved_doesNotThrow() {
        assertThatCode(() -> adapter.publishOrderReturnApproved("o1", "rr1", "u1", "e@e.com", "NX-1", "100.00"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishCartAbandoned_doesNotThrow() {
        assertThatCode(() -> adapter.publishCartAbandoned("c1", "u1", "e@e.com", "100.00", 3, "2026-01-01T00:00:00Z"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishCartCheckoutInitiated_doesNotThrow() {
        assertThatCode(() -> adapter.publishCartCheckoutInitiated("c1", "o1", "u1", "e@e.com", "100.00", "USD", 3,
                "WELCOME", "addr-1")).doesNotThrowAnyException();
    }

    @Test
    void publishStockReservation_doesNotThrow() {
        assertThatCode(() -> adapter.publishStockReservation("p1", "v1", "o1", 5)).doesNotThrowAnyException();
    }

    @Test
    void publishStockDeducted_doesNotThrow() {
        assertThatCode(() -> adapter.publishStockDeducted("p1", "v1", "o1", 5)).doesNotThrowAnyException();
    }

    @Test
    void publishInvoiceEmail_doesNotThrow() {
        assertThatCode(() -> adapter.publishInvoiceEmail("e@e.com", "Subject", "tpl", Map.of("k", "v")))
                .doesNotThrowAnyException();
    }

    @Test
    void publishSagaPaymentRequested_doesNotThrow() {
        assertThatCode(() -> adapter.publishSagaPaymentRequested("o1", "u1", "e@e.com", "NX-1", "100.00", "USD"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishSagaStockRelease_doesNotThrow() {
        assertThatCode(() -> adapter.publishSagaStockRelease("o1", "u1", "payment-failed")).doesNotThrowAnyException();
    }

    @Test
    void publishSagaNotifyFailure_doesNotThrow() {
        assertThatCode(() -> adapter.publishSagaNotifyFailure("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "reason"))
                .doesNotThrowAnyException();
    }

    @Test
    void publishCjOrderPaid_doesNotThrow() {
        assertThatCode(() -> adapter.publishCjOrderPaid("o1", "100.00")).doesNotThrowAnyException();
    }

    @Test
    void publishCjOrderAwaitingFunds_doesNotThrow() {
        assertThatCode(() -> adapter.publishCjOrderAwaitingFunds("o1", "100.00", "20.00")).doesNotThrowAnyException();
    }

    @Test
    void publishCjFulfillmentFailed_doesNotThrow() {
        assertThatCode(() -> adapter.publishCjFulfillmentFailed("o1", "PAY", "balance-low")).doesNotThrowAnyException();
    }

    @Test
    void publishCjBalanceLow_doesNotThrow() {
        assertThatCode(() -> adapter.publishCjBalanceLow("10.00", "50.00")).doesNotThrowAnyException();
    }

    @Test
    void publishCatalogProductUpdate_doesNotThrow() {
        assertThatCode(() -> adapter.publishCatalogProductUpdate("p1", "{\"sku\":\"x\"}")).doesNotThrowAnyException();
    }

    @Test
    void publishCatalogProductDelete_doesNotThrow() {
        assertThatCode(() -> adapter.publishCatalogProductDelete("p1")).doesNotThrowAnyException();
    }

    @Test
    void publishCatalogStockChange_doesNotThrow() {
        assertThatCode(() -> adapter.publishCatalogStockChange("v1", 100, "{\"raw\":1}")).doesNotThrowAnyException();
    }
}
