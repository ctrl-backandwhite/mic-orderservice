package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.OrderFinancialLedgerService.ReconciliationResult;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemSnapshotEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaymentReconciliationService")
class OrderPaymentReconciliationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderFinancialLedgerService ledgerService;
    @Mock
    private OrderItemSnapshotService snapshotService;
    @Mock
    private OrderStateHistoryService stateHistoryService;

    @InjectMocks
    private OrderPaymentReconciliationService service;

    private Order baseOrder() {
        OrderItem item = OrderItem.builder().id("it-1").orderId("ord-1").productId("p1").variantId("v1")
                .productName("Widget").productImage("u").unitPrice(Money.of(new BigDecimal("12"))).quantity(2).build();
        return Order.builder().id("ord-1").status(OrderStatus.PENDING).currencyCode("USD")
                .exchangeRateToUsd(BigDecimal.ONE).items(List.of(item)).build();
    }

    @Test
    @DisplayName("returns false when order not found")
    void noOrder() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());
        assertThat(service.onPaymentConfirmed("missing", "p", BigDecimal.TEN, "USD", "stripe", "t1")).isFalse();
    }

    @Test
    @DisplayName("acceptable margin returns true and skips NEEDS_REVIEW transition")
    void acceptable() {
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(baseOrder()));
        when(ledgerService.reconcile(eq("ord-1"), any())).thenReturn(new ReconciliationResult(new BigDecimal("100"),
                new BigDecimal("20"), new BigDecimal("80"), new BigDecimal("3"), true));
        boolean ok = service.onPaymentConfirmed("ord-1", "pay", new BigDecimal("100"), "USD", "stripe", "t1");
        assertThat(ok).isTrue();
        verify(snapshotService, times(1)).save(any(OrderItemSnapshotEntity.class));
        verify(stateHistoryService, never()).recordOrderTransition(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("unacceptable margin returns false and writes a NEEDS_REVIEW transition")
    void notAcceptable() {
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(baseOrder()));
        when(ledgerService.reconcile(eq("ord-1"), any())).thenReturn(new ReconciliationResult(new BigDecimal("10"),
                new BigDecimal("100"), new BigDecimal("-90"), new BigDecimal("3"), false));
        boolean ok = service.onPaymentConfirmed("ord-1", "pay", new BigDecimal("10"), "USD", "stripe", "t1");
        assertThat(ok).isFalse();
        verify(stateHistoryService).recordOrderTransition(eq("ord-1"), any(), eq("NEEDS_REVIEW"),
                eq(OrderStateHistoryService.ACTOR_SYSTEM), eq("reconciliation"), any(), eq(null));
    }

    @Test
    @DisplayName("handles orders with no items by skipping snapshots")
    void noItems() {
        Order empty = baseOrder().withItems(null);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(empty));
        when(ledgerService.reconcile(eq("ord-1"), any())).thenReturn(new ReconciliationResult(BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("3"), false));
        service.onPaymentConfirmed("ord-1", "pay", BigDecimal.ZERO, "USD", "stripe", "t");
        verify(snapshotService, never()).save(any());
    }

    @Test
    @DisplayName("uses ONE as fxRate fallback when not set on the order")
    void fxFallback() {
        Order o = baseOrder().withExchangeRateToUsd(null);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(o));
        when(ledgerService.reconcile(eq("ord-1"), any())).thenReturn(new ReconciliationResult(new BigDecimal("100"),
                new BigDecimal("20"), new BigDecimal("80"), new BigDecimal("3"), true));
        service.onPaymentConfirmed("ord-1", "pay", new BigDecimal("100"), "USD", "stripe", "t");
        verify(ledgerService).recordInbound(eq("ord-1"), any(), eq("USD"), eq(BigDecimal.ONE), eq("stripe"), eq("t"),
                any());
    }

    @Test
    @DisplayName("items with null unitPrice are skipped without crashing")
    void nullPriceItems() {
        OrderItem broken = OrderItem.builder().id("it-2").quantity(1).build();
        Order o = baseOrder().withItems(List.of(broken));
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(o));
        when(ledgerService.reconcile(eq("ord-1"), any())).thenReturn(
                new ReconciliationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("3"), true));
        boolean ok = service.onPaymentConfirmed("ord-1", "pay", BigDecimal.ZERO, "USD", "stripe", "t");
        assertThat(ok).isTrue();
        // Snapshot writes 0 priceCustomer because Money has no amount
        verify(snapshotService, times(1)).save(any());
    }

    @Test
    @DisplayName("estimateExpectedCjCostUsd returns total when fxRate is zero (no division)")
    void zeroFxRateInEstimate() throws Exception {
        Order o = baseOrder().withExchangeRateToUsd(BigDecimal.ZERO);
        // Force the zero-fx-rate branch in estimateExpectedCjCostUsd via reflection on
        // the private method.
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("estimateExpectedCjCostUsd", Order.class,
                BigDecimal.class);
        m.setAccessible(true);
        Object result = m.invoke(service, o, BigDecimal.ZERO);
        assertThat((BigDecimal) result).isEqualByComparingTo("24"); // 12 * 2
    }

    @Test
    @DisplayName("estimateExpectedCjCostUsd returns ZERO when items are null")
    void nullItemsInEstimate() throws Exception {
        Order o = baseOrder().withItems(null);
        java.lang.reflect.Method m = service.getClass().getDeclaredMethod("estimateExpectedCjCostUsd", Order.class,
                BigDecimal.class);
        m.setAccessible(true);
        Object result = m.invoke(service, o, BigDecimal.ONE);
        assertThat((BigDecimal) result).isEqualByComparingTo("0");
    }
}
