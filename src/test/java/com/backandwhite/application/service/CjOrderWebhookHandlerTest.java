package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.webhook.CjOrderSplitWebhookParams;
import com.backandwhite.api.dto.webhook.CjOrderWebhookParams;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderSplitEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderSplitJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjOrderWebhookHandler")
class CjOrderWebhookHandlerTest {

    @Mock
    private CjOrderRepository cjOrderRepository;
    @Mock
    private CjOrderSplitJpaRepository splitRepository;

    @InjectMocks
    private CjOrderWebhookHandler handler;

    @Test
    @DisplayName("handleOrderEvent updates status and tracking when found")
    void happyPath() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("SHIPPED");
        p.setTrackNumber("TRK-1");
        p.setLogisticName("PostNL");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.SHIPPED);
        assertThat(order.getTrackNumber()).isEqualTo("TRK-1");
        assertThat(order.getLogisticName()).isEqualTo("PostNL");
        verify(cjOrderRepository).save(order);
    }

    @Test
    @DisplayName("handleOrderEvent ignores unknown CJ order IDs")
    void orderUnknown() {
        when(cjOrderRepository.findByCjOrderId("cj-x")).thenReturn(Optional.empty());
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-x");
        handler.handleOrderEvent("ORDER", p);
        verify(cjOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("unmapped statuses are silently ignored")
    void unmappedStatus() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("MYSTERY");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isNull();
        verify(cjOrderRepository).save(order);
    }

    @Test
    @DisplayName("blank tracking number / logistic-name fields are skipped")
    void blanksSkipped() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").trackNumber("OLD").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("CREATED");
        p.setTrackNumber("");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getTrackNumber()).isEqualTo("OLD");
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.CREATED);
    }

    @Test
    @DisplayName("null status is treated as no-op for the status field")
    void nullStatus() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus(null);
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isNull();
    }

    @Test
    @DisplayName("handleOrderSplitEvent stores each sub-order and updates parent")
    void splitHappyPath() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderSplitWebhookParams params = new CjOrderSplitWebhookParams();
        params.setOrderId("cj-1");
        CjOrderSplitWebhookParams.SubOrder sub = new CjOrderSplitWebhookParams.SubOrder();
        sub.setCjOrderId("cj-child-1");
        sub.setOrderStatus("CREATED");
        sub.setProductList("[]");
        params.setSubOrders(List.of(sub));
        handler.handleOrderSplitEvent(params);
        verify(splitRepository, times(1)).save(any(CjOrderSplitEntity.class));
        verify(cjOrderRepository).save(order);
        assertThat(order.getLastWebhookAt()).isNotNull();
    }

    @Test
    @DisplayName("split for unknown parent is ignored")
    void splitUnknownParent() {
        when(cjOrderRepository.findByCjOrderId("cj-x")).thenReturn(Optional.empty());
        CjOrderSplitWebhookParams params = new CjOrderSplitWebhookParams();
        params.setOrderId("cj-x");
        handler.handleOrderSplitEvent(params);
        verify(splitRepository, never()).save(any());
        verify(cjOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("split with null sub-orders only updates parent timestamp")
    void splitNullSubOrders() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderSplitWebhookParams params = new CjOrderSplitWebhookParams();
        params.setOrderId("cj-1");
        params.setSubOrders(null);
        handler.handleOrderSplitEvent(params);
        verify(splitRepository, never()).save(any());
        verify(cjOrderRepository).save(order);
    }

    @Test
    @DisplayName("IN_PRODUCTION status maps to CjOrderStatus.IN_PRODUCTION")
    void inProductionStatus() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("IN_PRODUCTION");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.IN_PRODUCTION);
    }

    @Test
    @DisplayName("DELIVERED status maps to CjOrderStatus.DELIVERED")
    void deliveredStatus() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("delivered");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("CANCELLED status maps to CjOrderStatus.CANCELLED")
    void cancelledStatus() {
        CjOrder order = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjOrderId("cj-1")).thenReturn(Optional.of(order));
        CjOrderWebhookParams p = new CjOrderWebhookParams();
        p.setOrderId("cj-1");
        p.setOrderStatus("CANCELLED");
        handler.handleOrderEvent("ORDER", p);
        assertThat(order.getCjOrderStatus()).isEqualTo(CjOrderStatus.CANCELLED);
    }
}
