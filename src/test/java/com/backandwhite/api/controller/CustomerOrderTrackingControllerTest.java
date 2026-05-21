package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backandwhite.application.service.OrderItemSnapshotService;
import com.backandwhite.application.service.OrderStateHistoryService;
import com.backandwhite.application.service.TrackingUrlSigner;
import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerOrderTrackingControllerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CjOrderRepository cjOrderRepository;
    @Mock
    private TrackingUseCase trackingUseCase;
    @Mock
    private OrderStateHistoryService stateHistoryService;
    @Mock
    private OrderItemSnapshotService snapshotService;
    @Mock
    private TrackingUrlSigner trackingUrlSigner;

    @InjectMocks
    private CustomerOrderTrackingController controller;

    @Test
    void tracking_returnsPayload() {
        Order order = Order.builder().id("o1").status(OrderStatus.CONFIRMED).build();
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").cjOrderId("CJ-1")
                .cjOrderStatus(CjOrderStatus.CREATED).trackNumber("TRK").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        when(trackingUseCase.findByOrderId("o1")).thenReturn(List.of());
        when(stateHistoryService.findOrderHistory("o1")).thenReturn(List.of());
        when(snapshotService.findByOrder("o1")).thenReturn(List.of());

        var resp = controller.tracking("o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("orderId", "o1").containsEntry("orderStatus", "CONFIRMED")
                .containsEntry("cjOrderId", "CJ-1").containsEntry("trackNumber", "TRK");
    }

    @Test
    void tracking_orderMissing_throws() {
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.tracking("o1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tracking_noCjOrder_returnsEmptyCjFields() {
        Order order = Order.builder().id("o1").status(null).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        when(trackingUseCase.findByOrderId("o1")).thenReturn(List.of());
        when(stateHistoryService.findOrderHistory("o1")).thenReturn(List.of());
        when(snapshotService.findByOrder("o1")).thenReturn(List.of());

        var resp = controller.tracking("o1");
        assertThat(resp.getBody()).containsEntry("cjOrderId", "").containsEntry("cjStatus", "")
                .containsEntry("trackNumber", "").containsEntry("orderStatus", "");
    }

    @Test
    void publicTracking_invalidSignature_returns404() {
        when(trackingUrlSigner.verify("o1", 12345L, "bad")).thenReturn(false);
        var resp = controller.publicTracking("o1", 12345L, "bad");
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void publicTracking_validSig_returnsPayload() {
        when(trackingUrlSigner.verify("o1", 12345L, "ok")).thenReturn(true);
        Order order = Order.builder().id("o1").status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        when(trackingUseCase.findByOrderId("o1")).thenReturn(List.of());
        when(stateHistoryService.findOrderHistory("o1")).thenReturn(List.of());
        when(snapshotService.findByOrder("o1")).thenReturn(List.of());

        var resp = controller.publicTracking("o1", 12345L, "ok");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void publicTracking_validSig_orderMissing_returns404() {
        when(trackingUrlSigner.verify("o1", 12345L, "ok")).thenReturn(true);
        when(orderRepository.findById("o1")).thenReturn(Optional.empty());
        var resp = controller.publicTracking("o1", 12345L, "ok");
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }
}
