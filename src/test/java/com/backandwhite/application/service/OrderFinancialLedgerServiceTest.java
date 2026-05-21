package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.db.postgres.entity.OrderFinancialLedgerEntity;
import com.backandwhite.infrastructure.db.postgres.repository.OrderFinancialLedgerJpaRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFinancialLedgerService")
class OrderFinancialLedgerServiceTest {

    @Mock
    private OrderFinancialLedgerJpaRepository repository;

    @InjectMocks
    private OrderFinancialLedgerService service;

    @BeforeEach
    void setUp() throws Exception {
        Field f = OrderFinancialLedgerService.class.getDeclaredField("minGrossMarginUsd");
        f.setAccessible(true);
        f.set(service, new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("marks order as acceptable when gross margin exceeds threshold")
    void reconcilesWithAcceptableMargin() {
        when(repository.findAllByOrderIdOrderByCreatedAtAsc("ord-1"))
                .thenReturn(List.of(OrderFinancialLedgerEntity.builder().entryType("INBOUND")
                        .amount(new BigDecimal("30.00")).currency("USD").fxRate(BigDecimal.ONE).build()));

        var result = service.reconcile("ord-1", new BigDecimal("20.00"));

        assertThat(result.grossMarginUsd()).isEqualByComparingTo("10.00");
        assertThat(result.acceptable()).isTrue();
    }

    @Test
    @DisplayName("flags order as NOT acceptable when refunds eat the margin")
    void reconcilesWithNegativeMargin() {
        when(repository.findAllByOrderIdOrderByCreatedAtAsc("ord-2")).thenReturn(List.of(
                OrderFinancialLedgerEntity.builder().entryType("INBOUND").amount(new BigDecimal("30.00"))
                        .currency("USD").fxRate(BigDecimal.ONE).build(),
                OrderFinancialLedgerEntity.builder().entryType("REFUND").amount(new BigDecimal("25.00")).currency("USD")
                        .fxRate(BigDecimal.ONE).build()));

        var result = service.reconcile("ord-2", new BigDecimal("20.00"));

        assertThat(result.acceptable()).isFalse();
        assertThat(result.grossMarginUsd()).isLessThan(new BigDecimal("0"));
    }

    @Test
    @DisplayName("recordInbound persists an INBOUND entry with the provided fx rate")
    void recordsInbound() {
        when(repository.save(org.mockito.ArgumentMatchers.any(OrderFinancialLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        OrderFinancialLedgerEntity saved = service.recordInbound("o-1", new BigDecimal("100"), "USD", BigDecimal.ONE,
                "stripe", "tx-1", "{}");
        assertThat(saved.getEntryType()).isEqualTo("INBOUND");
        assertThat(saved.getProvider()).isEqualTo("stripe");
        assertThat(saved.getFxRate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("recordOutbound saves an OUTBOUND entry tagged 'cj'")
    void recordsOutbound() {
        when(repository.save(org.mockito.ArgumentMatchers.any(OrderFinancialLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        OrderFinancialLedgerEntity saved = service.recordOutbound("o-2", new BigDecimal("50"), "USD", "pay-1", "{}");
        assertThat(saved.getEntryType()).isEqualTo("OUTBOUND");
        assertThat(saved.getProvider()).isEqualTo("cj");
        assertThat(saved.getExternalTxId()).isEqualTo("pay-1");
    }

    @Test
    @DisplayName("recordRefund saves a REFUND entry with the supplied provider")
    void recordsRefund() {
        when(repository.save(org.mockito.ArgumentMatchers.any(OrderFinancialLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        OrderFinancialLedgerEntity saved = service.recordRefund("o-3", new BigDecimal("25"), "USD", "stripe", "rf-1",
                "{\"reason\":\"requested\"}");
        assertThat(saved.getEntryType()).isEqualTo("REFUND");
        assertThat(saved.getProvider()).isEqualTo("stripe");
    }

    @Test
    @DisplayName("recordInbound substitutes ONE when fxRate is null")
    void inboundDefaultsFxRate() {
        when(repository.save(org.mockito.ArgumentMatchers.any(OrderFinancialLedgerEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        OrderFinancialLedgerEntity saved = service.recordInbound("o-4", new BigDecimal("10"), "EUR", null, "stripe",
                "tx-2", null);
        assertThat(saved.getFxRate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("findByOrder delegates to the JPA repository")
    void findByOrderDelegates() {
        when(repository.findAllByOrderIdOrderByCreatedAtAsc("o-x")).thenReturn(List.of());
        assertThat(service.findByOrder("o-x")).isEmpty();
    }

    @Test
    @DisplayName("toUsd substitutes ONE when entry fxRate is null")
    void toUsdDefaultFx() {
        when(repository.findAllByOrderIdOrderByCreatedAtAsc("ord-fx")).thenReturn(List.of(OrderFinancialLedgerEntity
                .builder().entryType("INBOUND").amount(new BigDecimal("50.00")).currency("USD").fxRate(null).build()));
        var result = service.reconcile("ord-fx", new BigDecimal("10"));
        assertThat(result.netInboundUsd()).isEqualByComparingTo("50.0000");
    }
}
