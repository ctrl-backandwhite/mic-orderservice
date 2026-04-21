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
}
