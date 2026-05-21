package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjBalanceAdminControllerTest {

    @Mock
    private CjShoppingPort cjShoppingPort;

    @InjectMocks
    private CjBalanceAdminController controller;

    @BeforeEach
    void setUp() throws Exception {
        Field f = CjBalanceAdminController.class.getDeclaredField("minBalanceAlert");
        f.setAccessible(true);
        f.set(controller, new BigDecimal("50.00"));
    }

    @Test
    void current_aboveThreshold_returnsBalanceAndFalse() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100.00"));
        var resp = controller.current();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("balanceUsd", new BigDecimal("100.00")).containsEntry("belowThreshold",
                false);
    }

    @Test
    void current_belowThreshold_returnsTrue() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("10.00"));
        var resp = controller.current();
        assertThat(resp.getBody()).containsEntry("belowThreshold", true);
    }

    @Test
    void current_nullBalance_returnsZero() {
        when(cjShoppingPort.getBalanceAmount()).thenReturn(null);
        var resp = controller.current();
        assertThat(resp.getBody()).containsEntry("balanceUsd", BigDecimal.ZERO).containsEntry("belowThreshold", false);
    }
}
