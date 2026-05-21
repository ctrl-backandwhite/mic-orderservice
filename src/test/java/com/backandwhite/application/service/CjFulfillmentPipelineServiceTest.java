package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjFulfillmentPipelineService")
class CjFulfillmentPipelineServiceTest {

    @Mock
    private CjShoppingPort cjShoppingPort;

    @Mock
    private CjOrderRepository cjOrderRepository;

    @Mock
    private OrderEventPort orderEventPort;

    @InjectMocks
    private CjFulfillmentPipelineService service;

    @BeforeEach
    void setUp() throws Exception {
        Field f = CjFulfillmentPipelineService.class.getDeclaredField("minBalanceAlert");
        f.setAccessible(true);
        f.set(service, new BigDecimal("50"));
    }

    private CjOrder baseOrder() {
        return CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").errorCount(0).build();
    }

    private CjFulfillmentResult ok() {
        return CjFulfillmentResult.builder().success(true).shipmentsId("ship-1").payId("pay-1")
                .actualPayment(new BigDecimal("10")).postage(new BigDecimal("3")).productAmount(new BigDecimal("7"))
                .build();
    }

    private CjFulfillmentResult fail(String reason) {
        return CjFulfillmentResult.builder().success(false).errorReason(reason).build();
    }

    @Test
    @DisplayName("processFulfillment runs all 4 steps when CJ replies OK")
    void happyPath() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        service.processFulfillment(order);
        verify(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        verify(orderEventPort).publishCjOrderPaid(eq("ord-1"), anyString());
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
    }

    @Test
    @DisplayName("addCart failure short-circuits the pipeline")
    void addCartFailure() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(fail("addCart failed"));
        service.processFulfillment(order);
        verify(orderEventPort).publishCjFulfillmentFailed("ord-1", "ADD_CART", "addCart failed");
        verify(cjShoppingPort, never()).addCartConfirm(anyString());
    }

    @Test
    @DisplayName("addCartConfirm failure short-circuits the pipeline")
    void cartConfirmFailure() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(fail("confirm failed"));
        service.processFulfillment(order);
        verify(orderEventPort).publishCjFulfillmentFailed("ord-1", "CART_CONFIRM", "confirm failed");
        verify(cjShoppingPort, never()).generateParentOrder(anyString());
    }

    @Test
    @DisplayName("generateParentOrder failure short-circuits the pipeline")
    void genOrderFailure() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(fail("gen failed"));
        service.processFulfillment(order);
        verify(orderEventPort).publishCjFulfillmentFailed("ord-1", "GEN_ORDER", "gen failed");
        verify(cjShoppingPort, never()).payBalanceV2(anyString(), anyString());
    }

    @Test
    @DisplayName("AWAITING_FUNDS published when CJ balance is too low")
    void awaitingFunds() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("1"));
        service.processFulfillment(order);
        verify(orderEventPort).publishCjOrderAwaitingFunds(eq("ord-1"), anyString(), anyString());
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.AWAITING_FUNDS);
        verify(cjShoppingPort, never()).payBalanceV2(anyString(), anyString());
    }

    @Test
    @DisplayName("payBalance exception is recorded as PIPELINE_FAILED")
    void payFailure() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("999"));
        doThrow(new RuntimeException("pay-error")).when(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        service.processFulfillment(order);
        verify(orderEventPort).publishCjFulfillmentFailed("ord-1", "PAY", "pay-error");
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.PIPELINE_FAILED);
    }

    @Test
    @DisplayName("resumeFulfillment restarts from scratch when step is null")
    void resumeFromNull() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep(null);
        when(cjShoppingPort.addCart("cj-1")).thenReturn(fail("again"));
        service.resumeFulfillment(order);
        verify(cjShoppingPort).addCart("cj-1");
    }

    @Test
    @DisplayName("resumeFulfillment from CART_CONFIRM skips addCart")
    void resumeFromConfirm() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("CART_CONFIRM");
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(fail("still bad"));
        service.resumeFulfillment(order);
        verify(cjShoppingPort, never()).addCart(anyString());
        verify(cjShoppingPort).addCartConfirm("cj-1");
    }

    @Test
    @DisplayName("resumeFulfillment from GEN_ORDER skips addCart and confirm")
    void resumeFromGenOrder() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("GEN_ORDER");
        order.setShipmentsId("ship-1");
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(fail("bad"));
        service.resumeFulfillment(order);
        verify(cjShoppingPort, never()).addCart(anyString());
        verify(cjShoppingPort).generateParentOrder("ship-1");
    }

    @Test
    @DisplayName("resumeFulfillment from PAY skips earlier steps and re-tries pay")
    void resumeFromPay() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("PAY");
        order.setShipmentsId("ship-1");
        order.setPayId("pay-1");
        order.setCjActualPayment(new BigDecimal("10"));
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        service.resumeFulfillment(order);
        verify(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
    }

    @Test
    @DisplayName("resumeFulfillment from PAY surfaces AWAITING_FUNDS when balance too low")
    void resumePayAwaitingFunds() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("PAY");
        order.setShipmentsId("ship-1");
        order.setPayId("pay-1");
        order.setCjActualPayment(new BigDecimal("100"));
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("1"));
        service.resumeFulfillment(order);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.AWAITING_FUNDS);
        verify(cjShoppingPort, never()).payBalanceV2(anyString(), anyString());
    }

    @Test
    @DisplayName("resumeFulfillment from unknown step restarts from scratch")
    void resumeUnknownStep() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("WHATEVER");
        when(cjShoppingPort.addCart("cj-1")).thenReturn(fail("x"));
        service.resumeFulfillment(order);
        verify(cjShoppingPort).addCart("cj-1");
    }

    @Test
    @DisplayName("publishCjOrderPaid swallows event-port exceptions")
    void publishCjOrderPaidSwallows() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        doThrow(new RuntimeException("kafka")).when(orderEventPort).publishCjOrderPaid(anyString(), anyString());
        service.processFulfillment(order);
        verify(orderEventPort).publishCjOrderPaid(anyString(), anyString());
    }

    @Test
    @DisplayName("markWebhookReceived stamps lastWebhookAt and saves")
    void markWebhook() {
        CjOrder order = baseOrder();
        service.markWebhookReceived(order);
        assertThat(order.getLastWebhookAt()).isNotNull();
        verify(cjOrderRepository).save(order);
    }

    @Test
    @DisplayName("publishAwaitingFunds swallows event-port failures")
    void awaitingFundsEventFailureSwallowed() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(ok());
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("0"));
        doThrow(new RuntimeException("k")).when(orderEventPort).publishCjOrderAwaitingFunds(anyString(), anyString(),
                anyString());
        service.processFulfillment(order);
        verify(cjShoppingPort, never()).payBalanceV2(anyString(), anyString());
    }

    @Test
    @DisplayName("recordPipelineFailure increments errorCount and logs reason")
    void failureIncrementsErrorCount() {
        CjOrder order = baseOrder();
        order.setErrorCount(2);
        when(cjShoppingPort.addCart("cj-1")).thenReturn(fail("x"));
        service.processFulfillment(order);
        assertThat(order.getErrorCount()).isEqualTo(3);
        verify(cjOrderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("publishFulfillmentFailed swallows event-port failures")
    void publishFailedSwallows() {
        CjOrder order = baseOrder();
        when(cjShoppingPort.addCart("cj-1")).thenReturn(fail("x"));
        doThrow(new RuntimeException("k")).when(orderEventPort).publishCjFulfillmentFailed(anyString(), anyString(),
                any());
        service.processFulfillment(order);
        verify(orderEventPort).publishCjFulfillmentFailed(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("resumeFromConfirm continues into gen-order on success")
    void resumeFromConfirmSuccess() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("CART_CONFIRM");
        when(cjShoppingPort.addCartConfirm("cj-1")).thenReturn(ok());
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        service.resumeFulfillment(order);
        assertThat(order.getShipmentsId()).isEqualTo("ship-1");
        verify(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
    }

    @Test
    @DisplayName("resumeFromGenOrder continues into pay on success")
    void resumeFromGenOrderSuccess() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("GEN_ORDER");
        order.setShipmentsId("ship-1");
        when(cjShoppingPort.generateParentOrder("ship-1")).thenReturn(ok());
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        service.resumeFulfillment(order);
        assertThat(order.getPayId()).isEqualTo("pay-1");
        assertThat(order.getCjActualPayment()).isEqualByComparingTo(new BigDecimal("10"));
        verify(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
    }

    @Test
    @DisplayName("resumeFromPay records PIPELINE_FAILED when payBalanceV2 throws")
    void resumeFromPayException() {
        CjOrder order = baseOrder();
        order.setFulfillmentStep("PAY");
        order.setShipmentsId("ship-1");
        order.setPayId("pay-1");
        order.setCjActualPayment(new BigDecimal("10"));
        when(cjShoppingPort.getBalanceAmount()).thenReturn(new BigDecimal("100"));
        doThrow(new RuntimeException("net-fail")).when(cjShoppingPort).payBalanceV2("ship-1", "pay-1");
        service.resumeFulfillment(order);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.PIPELINE_FAILED);
        assertThat(order.getFulfillmentError()).contains("net-fail");
    }
}
