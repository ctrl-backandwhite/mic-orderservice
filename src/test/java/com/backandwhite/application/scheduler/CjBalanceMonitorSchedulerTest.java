package com.backandwhite.application.scheduler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CjBalanceMonitorSchedulerTest {

    @Mock
    private CjShoppingPort cjShoppingPort;
    @Mock
    private OrderEventPort orderEventPort;

    @InjectMocks
    private CjBalanceMonitorScheduler scheduler;

    @BeforeEach
    void setThreshold() {
        ReflectionTestUtils.setField(scheduler, "minBalanceAlert", new BigDecimal("50.00"));
    }

    @Test
    void checkBalance_balanceNull_skipsThresholdCheck() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(null);

        scheduler.checkBalance();

        verify(orderEventPort, never()).publishCjBalanceLow(anyString(), anyString());
    }

    @Test
    void checkBalance_aboveThreshold_doesNotPublish() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100.00"));

        scheduler.checkBalance();

        verify(orderEventPort, never()).publishCjBalanceLow(anyString(), anyString());
    }

    @Test
    void checkBalance_belowThreshold_publishesLowEvent() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("10.00"));

        scheduler.checkBalance();

        verify(orderEventPort).publishCjBalanceLow("10.00", "50.00");
    }

    @Test
    void checkBalance_atThreshold_doesNotPublish() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("50.00"));

        scheduler.checkBalance();

        verify(orderEventPort, never()).publishCjBalanceLow(anyString(), anyString());
    }

    @Test
    void checkBalance_swallowsExceptionFromPort() {
        when(cjShoppingPort.getBalanceAmount()).thenThrow(new RuntimeException("cj down"));

        // Should not throw — outer try-catch logs and exits.
        scheduler.checkBalance();

        verify(orderEventPort, never()).publishCjBalanceLow(anyString(), anyString());
    }
}
