package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.service.CjFulfillmentPipelineService;
import com.backandwhite.application.usecase.CjOrderFulfillmentUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjOrderAdminControllerTest {

    @Mock
    private CjOrderFulfillmentUseCase cjOrderFulfillmentUseCase;
    @Mock
    private CjFulfillmentPipelineService pipelineService;
    @Mock
    private CjShoppingPort cjShoppingPort;
    @Mock
    private CjOrderRepository cjOrderRepository;

    @InjectMocks
    private CjOrderAdminController controller;

    @Test
    void getByOrderId_returnsOk() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").build();
        when(cjOrderFulfillmentUseCase.findByOrderId("o1")).thenReturn(cjOrder);
        var resp = controller.getByOrderId("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getOrderId()).isEqualTo("o1");
    }

    @Test
    void syncStatus_returnsOk() {
        CjOrder synced = CjOrder.builder().id("c1").orderId("o1").build();
        when(cjOrderFulfillmentUseCase.syncStatus("o1")).thenReturn(synced);
        var resp = controller.syncStatus("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void submitToCj_returnsOk() {
        CjOrder submitted = CjOrder.builder().id("c1").orderId("o1").build();
        when(cjOrderFulfillmentUseCase.submitOrderToCj("o1")).thenReturn(submitted);
        var resp = controller.submitToCj("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void retryFulfillment_resumesPipeline() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        var resp = controller.retryFulfillment("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "pipeline resumed").containsEntry("orderId", "o1");
        verify(pipelineService).resumeFulfillment(cjOrder);
    }

    @Test
    void retryFulfillment_missingOrder_throws() {
        when(cjOrderRepository.findByOrderId("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.retryFulfillment("auth", "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelOrder_returnsOk() {
        var resp = controller.cancelOrder("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "cancelled").containsEntry("orderId", "o1");
        verify(cjOrderFulfillmentUseCase).cancelCjOrder("o1");
    }

    @Test
    void getBalance_returnsOk() {
        when(cjShoppingPort.getBalance()).thenReturn("50.00");
        var resp = controller.getBalance("auth");
        assertThat(resp.getBody()).containsEntry("balance", "50.00");
    }
}
