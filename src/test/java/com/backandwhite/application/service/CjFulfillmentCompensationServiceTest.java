package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjFulfillmentCompensationService")
class CjFulfillmentCompensationServiceTest {

    @Mock
    private CjShoppingPort cjShoppingPort;
    @Mock
    private CjOrderRepository cjOrderRepository;
    @Mock
    private OrderEventPort orderEventPort;
    @Mock
    private OrderStateHistoryService stateHistoryService;

    @InjectMocks
    private CjFulfillmentCompensationService service;

    @Test
    @DisplayName("on balance-insufficient, marks AWAITING_FUNDS without deleting the CJ order")
    void onBalanceInsufficient() {
        CjOrder cjOrder = CjOrder.builder().orderId("o1").cjOrderId("cj1").build();

        boolean handled = service.handlePipelineFailure(cjOrder, "PAY", "1604000", "balance insufficient");

        assertThat(handled).isTrue();
        assertThat(cjOrder.getFulfillmentStep()).isEqualTo("AWAITING_FUNDS");
        verify(cjShoppingPort, never()).deleteOrder(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("on irrecoverable code, deletes CJ order and fires refund event")
    void onIrrecoverable() {
        CjOrder cjOrder = CjOrder.builder().orderId("o2").cjOrderId("cj2").build();

        boolean handled = service.handlePipelineFailure(cjOrder, "CREATE", "1602003", "variant removed");

        assertThat(handled).isTrue();
        assertThat(cjOrder.getFulfillmentStep()).isEqualTo("FULFILLMENT_FAILED");
        verify(cjShoppingPort).deleteOrder("cj2");
        verify(orderEventPort).publishSagaNotifyFailure(org.mockito.ArgumentMatchers.eq("o2"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("o2"), org.mockito.ArgumentMatchers.eq("0"),
                org.mockito.ArgumentMatchers.eq("USD"),
                org.mockito.ArgumentMatchers.startsWith("CJ_FULFILLMENT_FAILED"));
    }

    @Test
    @DisplayName("returns false when CJ code is recoverable so caller may retry")
    void onRecoverableCode() {
        CjOrder cjOrder = CjOrder.builder().orderId("o3").cjOrderId("cj3").build();

        boolean handled = service.handlePipelineFailure(cjOrder, "CART", "1600200", "rate limited");

        assertThat(handled).isFalse();
    }

    @Test
    @DisplayName("returns false immediately when cjCode is null")
    void onNullCjCode() {
        CjOrder cjOrder = CjOrder.builder().orderId("o4").cjOrderId("cj4").build();

        boolean handled = service.handlePipelineFailure(cjOrder, "CART", null, "no code");

        assertThat(handled).isFalse();
        verify(cjShoppingPort, never()).deleteOrder(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("deleteOrder failures are swallowed during compensation")
    void deleteOrderFailureSwallowed() {
        CjOrder cjOrder = CjOrder.builder().orderId("o5").cjOrderId("cj5").build();
        org.mockito.Mockito.doThrow(new RuntimeException("network gone")).when(cjShoppingPort).deleteOrder("cj5");

        boolean handled = service.handlePipelineFailure(cjOrder, "CREATE", "1602001", "boom");

        assertThat(handled).isTrue();
        assertThat(cjOrder.getFulfillmentStep()).isEqualTo("FULFILLMENT_FAILED");
    }
}
