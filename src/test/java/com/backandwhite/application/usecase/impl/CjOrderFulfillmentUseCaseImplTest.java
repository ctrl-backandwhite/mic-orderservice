package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.exception.BusinessException;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjOrderFulfillmentUseCaseImplTest {

    @Mock
    private CjShoppingPort cjShoppingPort;

    @Mock
    private CjOrderRepository cjOrderRepository;

    @Mock
    private OrderUseCase orderUseCase;

    @Mock
    private CjFulfillmentPipelineService pipelineService;

    @InjectMocks
    private CjOrderFulfillmentUseCaseImpl useCase;

    @Test
    void submitOrderToCj_whenDisabled_noOpReturnsStub() throws Exception {
        // Flip the kill-switch off via reflection (same effect as
        // app.cj.enabled=false in application.yaml)
        java.lang.reflect.Field f = CjOrderFulfillmentUseCaseImpl.class.getDeclaredField("cjEnabled");
        f.setAccessible(true);
        f.setBoolean(useCase, false);

        CjOrder result = useCase.submitOrderToCj("o1");

        assertThat(result.getOrderId()).isEqualTo("o1");
        assertThat(result.getFulfillmentStep()).isEqualTo("DISABLED");
        verify(orderUseCase, never()).findById(anyString());
        verify(cjShoppingPort, never()).createOrder(any());
        verify(cjOrderRepository, never()).save(any());
        verify(pipelineService, never()).processFulfillment(any());
    }

    @Test
    void submitOrderToCj_notYetSubmitted_submitsAndSaves() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        CjOrder cj = CjOrder.builder().cjOrderId("CJ123").build();
        when(cjShoppingPort.createOrder(order)).thenReturn(cj);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CjOrder saved = useCase.submitOrderToCj("o1");

        assertThat(saved.getOrderId()).isEqualTo("o1");
        assertThat(saved.getCjOrderId()).isEqualTo("CJ123");
        assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getId()).isNotNull();
        verify(orderUseCase).updateCjFields("o1", "CJ123", null);
        verify(pipelineService).processFulfillment(saved);
    }

    @Test
    void submitOrderToCj_alreadySubmitted_throws() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        CjOrder existing = CjOrder.builder().cjOrderId("CJ1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.submitOrderToCj("o1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitOrderToCj_existingWithoutCjId_proceeds() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(CjOrder.builder().cjOrderId(null).build()));
        CjOrder cj = CjOrder.builder().cjOrderId("CJ1").build();
        when(cjShoppingPort.createOrder(order)).thenReturn(cj);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CjOrder saved = useCase.submitOrderToCj("o1");
        assertThat(saved.getCjOrderId()).isEqualTo("CJ1");
    }

    @Test
    void submitOrderToCj_noCjOrderIdReturned_skipsUpdateFields() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        CjOrder cj = CjOrder.builder().cjOrderId(null).build();
        when(cjShoppingPort.createOrder(order)).thenReturn(cj);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.submitOrderToCj("o1");
        verify(orderUseCase, never()).updateCjFields(anyString(), anyString(), any());
    }

    @Test
    void submitOrderToCj_pipelineFails_swallowsException() {
        Order order = Order.builder().id("o1").build();
        when(orderUseCase.findById("o1")).thenReturn(order);
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        CjOrder cj = CjOrder.builder().cjOrderId("CJ1").build();
        when(cjShoppingPort.createOrder(order)).thenReturn(cj);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("pipeline boom")).when(pipelineService).processFulfillment(any(CjOrder.class));

        CjOrder saved = useCase.submitOrderToCj("o1");
        assertThat(saved).isNotNull();
    }

    @Test
    void syncStatus_existingWithoutCjId_returnsExisting() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId(null).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));

        CjOrder result = useCase.syncStatus("o1");
        assertThat(result).isSameAs(existing);
    }

    @Test
    void syncStatus_missingRecord_throws() {
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.syncStatus("o1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void syncStatus_delivered_updatesInternalStatusAndTracking() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId("CJ1").build();
        CjOrder fresh = CjOrder.builder().cjOrderStatus(CjOrderStatus.DELIVERED).trackNumber("TR1").logisticName("DHL")
                .build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));
        when(cjShoppingPort.getOrderDetail("CJ1")).thenReturn(fresh);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.syncStatus("o1");

        verify(orderUseCase).updateStatus(eq("o1"), eq(OrderStatus.DELIVERED), eq("CJ_SYNC"), anyString());
        verify(orderUseCase).updateCjFields("o1", "CJ1", "TR1");
    }

    @Test
    void syncStatus_noInternalMapping_doesNotUpdate() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId("CJ1").build();
        CjOrder fresh = CjOrder.builder().cjOrderStatus(CjOrderStatus.CREATED).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));
        when(cjShoppingPort.getOrderDetail("CJ1")).thenReturn(fresh);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.syncStatus("o1");

        verify(orderUseCase, never()).updateStatus(anyString(), any(OrderStatus.class), anyString(), anyString());
    }

    @Test
    void syncStatus_updateStatusThrows_swallowed() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId("CJ1").build();
        CjOrder fresh = CjOrder.builder().cjOrderStatus(CjOrderStatus.SHIPPED).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));
        when(cjShoppingPort.getOrderDetail("CJ1")).thenReturn(fresh);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("boom")).when(orderUseCase).updateStatus(anyString(), any(OrderStatus.class),
                anyString(), anyString());

        useCase.syncStatus("o1");
    }

    @Test
    void syncStatus_nullStatus_skipsStatusAndTracking() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId("CJ1").build();
        CjOrder fresh = CjOrder.builder().cjOrderStatus(null).trackNumber(null).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));
        when(cjShoppingPort.getOrderDetail("CJ1")).thenReturn(fresh);
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.syncStatus("o1");

        verify(orderUseCase, never()).updateStatus(anyString(), any(OrderStatus.class), anyString(), anyString());
        verify(orderUseCase, never()).updateCjFields(anyString(), anyString(), anyString());
    }

    @Test
    void findByOrderId_existing_returns() {
        CjOrder c = CjOrder.builder().id("cj1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(c));
        assertThat(useCase.findByOrderId("o1")).isSameAs(c);
    }

    @Test
    void findByOrderId_missing_throws() {
        when(cjOrderRepository.findByOrderId("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findByOrderId("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void cancelCjOrder_existingWithCjId_cancels() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId("CJ1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));
        when(cjOrderRepository.save(any(CjOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.cancelCjOrder("o1");

        verify(cjShoppingPort).deleteOrder("CJ1");
        assertThat(existing.getCjOrderStatus()).isEqualTo(CjOrderStatus.CANCELLED);
    }

    @Test
    void cancelCjOrder_noCjId_doesNothing() {
        CjOrder existing = CjOrder.builder().orderId("o1").cjOrderId(null).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(existing));

        useCase.cancelCjOrder("o1");

        verify(cjShoppingPort, never()).deleteOrder(anyString());
    }

    @Test
    void cancelCjOrder_missing_throws() {
        when(cjOrderRepository.findByOrderId("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.cancelCjOrder("x")).isInstanceOf(BusinessException.class);
    }
}
