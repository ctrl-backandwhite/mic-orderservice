package com.backandwhite.application.service;

import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemSnapshotEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 5 + 8.3 wiring — invoked when a customer payment is confirmed. Writes
 * one INBOUND row to the financial ledger, persists an immutable snapshot of
 * every line item and reconciles the margin. If the margin falls below the
 * configured floor the order is flagged {@code NEEDS_REVIEW} and the CJ
 * pipeline is NOT dispatched, so we never ship at a loss.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OrderPaymentReconciliationService {

    private final OrderRepository orderRepository;
    private final OrderFinancialLedgerService ledgerService;
    private final OrderItemSnapshotService snapshotService;
    private final OrderStateHistoryService stateHistoryService;

    /**
     * @return {@code true} if reconciliation passed and the order may be released
     *         to CJ; {@code false} if the order was flagged NEEDS_REVIEW (margin
     *         below floor, missing FX, etc.).
     */
    @Transactional
    public boolean onPaymentConfirmed(String orderId, String paymentId, BigDecimal amount, String currency,
            String gateway, String transactionRef) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("::> Reconciliation skipped — order {} not found", orderId);
            return false;
        }

        BigDecimal fxRate = order.getExchangeRateToUsd() != null ? order.getExchangeRateToUsd() : BigDecimal.ONE;

        ledgerService.recordInbound(orderId, amount, currency, fxRate, gateway, transactionRef,
                "{\"paymentId\":\"" + paymentId + "\"}");

        persistSnapshots(order);

        BigDecimal expectedCjCostUsd = estimateExpectedCjCostUsd(order, fxRate);
        var result = ledgerService.reconcile(orderId, expectedCjCostUsd);
        log.info("::> Reconciliation orderId={} netInboundUsd={} expectedCjCostUsd={} grossMarginUsd={} acceptable={}",
                orderId, result.netInboundUsd(), result.expectedCjCostUsd(), result.grossMarginUsd(),
                result.acceptable());

        if (!result.acceptable()) {
            stateHistoryService
                    .recordOrderTransition(orderId, order.getStatus() != null ? order.getStatus().name() : null,
                            "NEEDS_REVIEW", OrderStateHistoryService.ACTOR_SYSTEM, "reconciliation", "Gross margin "
                                    + result.grossMarginUsd() + " USD below floor " + result.minRequiredMarginUsd(),
                            null);
        }
        return result.acceptable();
    }

    /** Writes one snapshot row per order item — run once per order at payment. */
    private void persistSnapshots(Order order) {
        if (order.getItems() == null) {
            return;
        }
        Instant now = Instant.now();
        for (OrderItem item : order.getItems()) {
            BigDecimal unitPrice = item.getUnitPrice() != null && item.getUnitPrice().getAmount() != null
                    ? item.getUnitPrice().getAmount()
                    : BigDecimal.ZERO;
            BigDecimal fx = order.getExchangeRateToUsd() != null ? order.getExchangeRateToUsd() : BigDecimal.ONE;
            BigDecimal priceUsd = unitPrice.divide(fx.compareTo(BigDecimal.ZERO) > 0 ? fx : BigDecimal.ONE, 4,
                    RoundingMode.HALF_UP);

            snapshotService.save(OrderItemSnapshotEntity.builder().orderId(order.getId()).orderItemId(item.getId())
                    .pid(item.getProductId()).vid(item.getVariantId()).name(item.getProductName())
                    .imageUrl(item.getProductImage()).priceCustomer(unitPrice).priceUsd(priceUsd)
                    .currency(order.getCurrencyCode() != null ? order.getCurrencyCode() : "USD").fxRate(fx)
                    .createdAt(now).build());
        }
    }

    /**
     * Rough expected CJ cost estimate: sum of item subtotals in USD. Does not
     * include real freight yet — for that we need the Fase 4 shipping-quote result
     * persisted on the order. Good enough to catch wildly-negative margins now.
     */
    private BigDecimal estimateExpectedCjCostUsd(Order order, BigDecimal fxRate) {
        if (order.getItems() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item.getUnitPrice() == null) {
                continue;
            }
            BigDecimal unit = item.getUnitPrice().getAmount() != null
                    ? item.getUnitPrice().getAmount()
                    : BigDecimal.ZERO;
            total = total.add(unit.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        if (fxRate == null || fxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return total;
        }
        return total.divide(fxRate, 4, RoundingMode.HALF_UP);
    }
}
