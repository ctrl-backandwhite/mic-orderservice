package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.OrderFinancialLedgerEntity;
import com.backandwhite.infrastructure.db.postgres.repository.OrderFinancialLedgerJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 5 — every dollar that moves for an order gets a row here. Customer
 * inbound, CJ payout, refund, chargeback. Lets Finance reconcile independently
 * from Stripe, PayPal or CJ weeks after the fact.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OrderFinancialLedgerService {

    public static final String INBOUND = "INBOUND";
    public static final String OUTBOUND = "OUTBOUND";
    public static final String REFUND = "REFUND";

    private final OrderFinancialLedgerJpaRepository repository;

    @Value("${app.cj.min-gross-margin-usd:3.00}")
    private BigDecimal minGrossMarginUsd;

    @Transactional
    public OrderFinancialLedgerEntity recordInbound(String orderId, BigDecimal amount, String currency,
            BigDecimal fxRate, String provider, String externalTxId, String metadata) {
        return record(orderId, INBOUND, amount, currency, fxRate, provider, externalTxId, metadata);
    }

    @Transactional
    public OrderFinancialLedgerEntity recordOutbound(String orderId, BigDecimal amount, String currency, String payId,
            String metadata) {
        return record(orderId, OUTBOUND, amount, currency, BigDecimal.ONE, "cj", payId, metadata);
    }

    @Transactional
    public OrderFinancialLedgerEntity recordRefund(String orderId, BigDecimal amount, String currency, String provider,
            String externalTxId, String metadata) {
        return record(orderId, REFUND, amount, currency, BigDecimal.ONE, provider, externalTxId, metadata);
    }

    public List<OrderFinancialLedgerEntity> findByOrder(String orderId) {
        return repository.findAllByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * Computes the current reconciled gross margin in USD given the ledger entries
     * and expected CJ cost. A negative or below-threshold result means the order
     * should not be released to CJ without manual review.
     */
    public ReconciliationResult reconcile(String orderId, BigDecimal expectedCjCostUsd) {
        BigDecimal inboundUsd = BigDecimal.ZERO;
        BigDecimal refundUsd = BigDecimal.ZERO;

        for (OrderFinancialLedgerEntity entry : findByOrder(orderId)) {
            BigDecimal usd = toUsd(entry);
            if (INBOUND.equals(entry.getEntryType())) {
                inboundUsd = inboundUsd.add(usd);
            } else if (REFUND.equals(entry.getEntryType())) {
                refundUsd = refundUsd.add(usd);
            }
        }

        BigDecimal netInboundUsd = inboundUsd.subtract(refundUsd);
        BigDecimal grossMarginUsd = netInboundUsd.subtract(expectedCjCostUsd);
        boolean acceptable = grossMarginUsd.compareTo(minGrossMarginUsd) >= 0;

        return new ReconciliationResult(netInboundUsd, expectedCjCostUsd, grossMarginUsd, minGrossMarginUsd,
                acceptable);
    }

    private OrderFinancialLedgerEntity record(String orderId, String type, BigDecimal amount, String currency,
            BigDecimal fxRate, String provider, String externalTxId, String metadata) {
        OrderFinancialLedgerEntity entity = OrderFinancialLedgerEntity.builder().orderId(orderId).entryType(type)
                .amount(amount).currency(currency).fxRate(fxRate != null ? fxRate : BigDecimal.ONE).provider(provider)
                .externalTxId(externalTxId).metadata(metadata).createdAt(Instant.now()).build();
        log.info("::> Ledger entry orderId={} type={} amount={} {} provider={}", orderId, type, amount, currency,
                provider);
        return repository.save(entity);
    }

    private static BigDecimal toUsd(OrderFinancialLedgerEntity e) {
        BigDecimal fx = e.getFxRate() != null ? e.getFxRate() : BigDecimal.ONE;
        return e.getAmount().multiply(fx).setScale(4, RoundingMode.HALF_UP);
    }

    public record ReconciliationResult(BigDecimal netInboundUsd, BigDecimal expectedCjCostUsd,
            BigDecimal grossMarginUsd, BigDecimal minRequiredMarginUsd, boolean acceptable) {
    }
}
