package com.backandwhite.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.domain.valueobject.OrderSagaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompensationService")
class OrderCompensationServiceTest {

    @Mock
    private OrderEventPort orderEventPort;
    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private OrderCompensationService service;

    @Test
    @DisplayName("compensate emits stock-release, notify-failure and toggles status")
    void happyPath() {
        service.compensate("ord-1", "u-1", "x@x", "ORD-001", "10.00", "USD", "reason-x");
        verify(orderUseCase).updateSagaStatus("ord-1", OrderSagaStatus.COMPENSATING);
        verify(orderEventPort).publishSagaStockRelease("ord-1", "u-1", "payment-failed");
        verify(orderEventPort).publishSagaNotifyFailure("ord-1", "u-1", "x@x", "ORD-001", "10.00", "USD", "reason-x");
        verify(orderUseCase).updateSagaStatus("ord-1", OrderSagaStatus.CANCELLED);
    }

    @Test
    @DisplayName("a failure setting COMPENSATING is logged but does not block compensation")
    void compensatingFailure() {
        doThrow(new RuntimeException("boom")).when(orderUseCase).updateSagaStatus("ord-1",
                OrderSagaStatus.COMPENSATING);
        service.compensate("ord-1", "u-1", "x@x", "ORD", "1", "USD", "r");
        verify(orderEventPort).publishSagaStockRelease("ord-1", "u-1", "payment-failed");
        verify(orderEventPort).publishSagaNotifyFailure(eq("ord-1"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("stock-release and notify-failure failures are caught independently")
    void publishFailuresHandled() {
        doThrow(new RuntimeException("kafka-down")).when(orderEventPort).publishSagaStockRelease(any(), any(), any());
        doThrow(new RuntimeException("kafka-down")).when(orderEventPort).publishSagaNotifyFailure(any(), any(), any(),
                any(), any(), any(), any());
        service.compensate("ord-1", "u-1", "x@x", "ORD", "1", "USD", "r");
        verify(orderUseCase, times(1)).updateSagaStatus("ord-1", OrderSagaStatus.CANCELLED);
    }

    @Test
    @DisplayName("CANCELLED status update failure is logged and ignored")
    void cancelledUpdateFailure() {
        doThrow(new RuntimeException("db")).when(orderUseCase).updateSagaStatus("ord-1", OrderSagaStatus.CANCELLED);
        service.compensate("ord-1", "u-1", "x@x", "ORD", "1", "USD", "r");
        verify(orderUseCase).updateSagaStatus("ord-1", OrderSagaStatus.CANCELLED);
    }
}
