package com.backandwhite.infrastructure.message.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.OrderCompensationService;
import com.backandwhite.application.service.OrderPaymentReconciliationService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.core.kafka.avro.PaymentConfirmedEvent;
import com.backandwhite.core.kafka.avro.PaymentFailedEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderDeliveredEvent;
import com.backandwhite.core.kafka.avro.ShippingOrderShippedEvent;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerServiceTest {

    @Mock
    private OrderUseCase orderUseCase;
    @Mock
    private OrderCompensationService orderCompensationService;
    @Mock
    private CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    @Mock
    private OrderPaymentReconciliationService reconciliationService;

    @InjectMocks
    private OrderEventConsumerService consumer;

    // ── onPaymentConfirmed ──────────────────────────────────────────────────

    private PaymentConfirmedEvent paymentConfirmed(String amount) {
        return PaymentConfirmedEvent.newBuilder().setPaymentId("pay-1").setOrderId("o1").setUserId("u1")
                .setEmail("e@e.com").setAmount(amount).setCurrency("USD").setMethod("STRIPE").setGateway("stripe")
                .setTransactionRef("tx-1").setTimestamp("2026-01-01T00:00:00Z").build();
    }

    @Test
    void onPaymentConfirmed_advancesDraftToPendingThenConfirmed_andSubmitsCj() {
        Order draft = Order.builder().id("o1").status(OrderStatus.DRAFT).build();
        when(orderUseCase.findById("o1")).thenReturn(draft);
        when(reconciliationService.onPaymentConfirmed(eq("o1"), eq("pay-1"), any(BigDecimal.class), eq("USD"),
                eq("stripe"), eq("tx-1"))).thenReturn(true);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(orderUseCase).updateStatus("o1", OrderStatus.PENDING, "SYSTEM",
                "Payment confirmed async — advancing DRAFT→PENDING");
        verify(orderUseCase).updateStatus("o1", OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");
        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
    }

    @Test
    void onPaymentConfirmed_skipsDraftStepWhenOrderNotInDraft() {
        Order pending = Order.builder().id("o1").status(OrderStatus.PENDING).build();
        when(orderUseCase.findById("o1")).thenReturn(pending);
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(true);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(orderUseCase, never()).updateStatus(eq("o1"), eq(OrderStatus.PENDING), anyString(), anyString());
        verify(orderUseCase).updateStatus("o1", OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");
        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
    }

    @Test
    void onPaymentConfirmed_findByIdNullSkipsDraftLadder() {
        when(orderUseCase.findById("o1")).thenReturn(null);
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(true);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(orderUseCase, never()).updateStatus(eq("o1"), eq(OrderStatus.PENDING), anyString(), anyString());
        verify(orderUseCase).updateStatus("o1", OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");
    }

    @Test
    void onPaymentConfirmed_swallowsDraftStepException() {
        when(orderUseCase.findById("o1")).thenThrow(new RuntimeException("db down"));
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(true);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(orderUseCase).updateStatus("o1", OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");
        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
    }

    @Test
    void onPaymentConfirmed_swallowsConfirmedStepException() {
        Order draft = Order.builder().id("o1").status(OrderStatus.DRAFT).build();
        when(orderUseCase.findById("o1")).thenReturn(draft);
        doThrow(new RuntimeException("already confirmed")).when(orderUseCase).updateStatus("o1", OrderStatus.CONFIRMED,
                "SYSTEM", "Payment confirmed");
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(true);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
    }

    @Test
    void onPaymentConfirmed_marginNotAcceptable_skipsCjSubmission() {
        when(orderUseCase.findById("o1")).thenReturn(null);
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(false);

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(cjOrderFulfillmentUseCase, never()).submitOrderToCj(anyString());
    }

    @Test
    void onPaymentConfirmed_reconciliationThrows_returnsFalseAndSkipsCj() {
        when(orderUseCase.findById("o1")).thenReturn(null);
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("recon boom"));

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(cjOrderFulfillmentUseCase, never()).submitOrderToCj(anyString());
    }

    @Test
    void onPaymentConfirmed_cjSubmissionFails_swallowsException() {
        when(orderUseCase.findById("o1")).thenReturn(null);
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(cjOrderFulfillmentUseCase.submitOrderToCj("o1")).thenThrow(new RuntimeException("cj down"));

        consumer.onPaymentConfirmed(paymentConfirmed("100.00"));

        verify(cjOrderFulfillmentUseCase).submitOrderToCj("o1");
    }

    @Test
    void onPaymentConfirmed_outerCatchHandlesUnexpectedFailure() {
        when(orderUseCase.findById("o1")).thenThrow(new RuntimeException("first"));
        // make second updateStatus blow up too — inner try-catches catch it,
        // but reconciliation throwing during BigDecimal parse triggers outer.
        when(reconciliationService.onPaymentConfirmed(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("kaboom"));

        // Should not throw.
        PaymentConfirmedEvent event = paymentConfirmed("100.00");
        assertThatCode(() -> consumer.onPaymentConfirmed(event)).doesNotThrowAnyException();
    }

    // ── onPaymentFailed ─────────────────────────────────────────────────────

    private PaymentFailedEvent paymentFailed() {
        return PaymentFailedEvent.newBuilder().setPaymentId("pay-1").setOrderId("o1").setUserId("u1")
                .setEmail("e@e.com").setAmount("100.00").setReason("declined").setGateway("stripe")
                .setTimestamp("2026-01-01T00:00:00Z").build();
    }

    @Test
    void onPaymentFailed_cancelsOrderAndCompensates() {
        consumer.onPaymentFailed(paymentFailed());

        verify(orderUseCase).cancel("o1", "u1", "Payment failed: declined");
        verify(orderCompensationService).compensate("o1", "u1", "e@e.com", "o1", "100.00", "USD", "declined");
    }

    @Test
    void onPaymentFailed_swallowsExceptionFromCancel() {
        doThrow(new RuntimeException("db down")).when(orderUseCase).cancel(eq("o1"), eq("u1"), anyString());

        consumer.onPaymentFailed(paymentFailed());

        verify(orderCompensationService, never()).compensate(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString());
    }

    // ── onShippingOrderShipped ──────────────────────────────────────────────

    private ShippingOrderShippedEvent shipped() {
        return ShippingOrderShippedEvent.newBuilder().setOrderId("o1").setUserId("u1").setEmail("e@e.com")
                .setTrackingNumber("TRACK-1").setCarrier("DHL").setEstimatedDelivery("2026-01-05")
                .setTimestamp("2026-01-01T00:00:00Z").build();
    }

    @Test
    void onShippingOrderShipped_advancesToShipped() {
        consumer.onShippingOrderShipped(shipped());

        verify(orderUseCase).updateStatus("o1", OrderStatus.SHIPPED, "SYSTEM", "Shipped via DHL tracking: TRACK-1");
    }

    @Test
    void onShippingOrderShipped_swallowsException() {
        doThrow(new RuntimeException("invalid transition")).when(orderUseCase).updateStatus(eq("o1"),
                eq(OrderStatus.SHIPPED), anyString(), anyString());
        ShippingOrderShippedEvent event = shipped();

        assertThatCode(() -> consumer.onShippingOrderShipped(event)).doesNotThrowAnyException();
    }

    // ── onShippingOrderDelivered ────────────────────────────────────────────

    private ShippingOrderDeliveredEvent delivered() {
        return ShippingOrderDeliveredEvent.newBuilder().setOrderId("o1").setUserId("u1").setEmail("e@e.com")
                .setTrackingNumber("TRACK-1").setCarrier("DHL").setDeliveredAt("2026-01-05T10:00:00Z")
                .setTimestamp("2026-01-05T10:00:00Z").build();
    }

    @Test
    void onShippingOrderDelivered_advancesToDelivered() {
        consumer.onShippingOrderDelivered(delivered());

        verify(orderUseCase).updateStatus("o1", OrderStatus.DELIVERED, "SYSTEM", "Delivered at 2026-01-05T10:00:00Z");
    }

    @Test
    void onShippingOrderDelivered_swallowsException() {
        doThrow(new RuntimeException("invalid transition")).when(orderUseCase).updateStatus(eq("o1"),
                eq(OrderStatus.DELIVERED), anyString(), anyString());
        ShippingOrderDeliveredEvent event = delivered();

        assertThatCode(() -> consumer.onShippingOrderDelivered(event)).doesNotThrowAnyException();
    }
}
