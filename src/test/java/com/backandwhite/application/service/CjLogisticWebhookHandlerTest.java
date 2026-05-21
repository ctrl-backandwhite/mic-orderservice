package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.webhook.CjLogisticWebhookParams;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.infrastructure.db.postgres.entity.CjTrackingEventEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjTrackingEventJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjLogisticWebhookHandler")
class CjLogisticWebhookHandlerTest {

    @Mock
    private CjOrderRepository cjOrderRepository;
    @Mock
    private OrderJpaRepository orderJpaRepository;
    @Mock
    private CjTrackingEventJpaRepository trackingEventRepository;

    @InjectMocks
    private CjLogisticWebhookHandler handler;

    private CjLogisticWebhookParams sampleParams() {
        CjLogisticWebhookParams p = new CjLogisticWebhookParams();
        p.setTrackingNumber("TRK-1");
        p.setStatus("IN_TRANSIT");
        p.setStatusDescription("On its way");
        p.setLogisticName("PostNL");
        p.setTrackUrl("https://track");
        p.setLastMileCarrier("DHL");
        p.setLastTrackNumber("LM-1");
        CjLogisticWebhookParams.TrackingEvent ev = new CjLogisticWebhookParams.TrackingEvent();
        ev.setActivity("Out for delivery");
        ev.setLocation("MAD");
        ev.setEventTime("2024-05-01 10:30:00");
        p.setEvents(List.of(ev));
        return p;
    }

    @Test
    @DisplayName("persists tracking events and enriches the order when matched")
    void happyPath() {
        CjOrder cj = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.of(cj));
        OrderEntity order = OrderEntity.builder().id("ord-1").build();
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(cjOrderRepository.findByOrderId("ord-1")).thenReturn(Optional.of(cj));

        handler.handleLogisticEvent(sampleParams());

        ArgumentCaptor<CjTrackingEventEntity> evCaptor = ArgumentCaptor.forClass(CjTrackingEventEntity.class);
        verify(trackingEventRepository).save(evCaptor.capture());
        assertThat(evCaptor.getValue().getOrderId()).isEqualTo("ord-1");
        assertThat(order.getTrackingUrl()).isEqualTo("https://track");
        assertThat(order.getLastMileCarrier()).isEqualTo("DHL");
        assertThat(order.getLastMileTrackNumber()).isEqualTo("LM-1");
        verify(cjOrderRepository).save(cj);
        assertThat(cj.getLastWebhookAt()).isNotNull();
    }

    @Test
    @DisplayName("orphan tracking events are stored without an internal orderId")
    void orphanTracking() {
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.empty());
        handler.handleLogisticEvent(sampleParams());
        verify(trackingEventRepository, times(1)).save(any(CjTrackingEventEntity.class));
        verify(orderJpaRepository, never()).findById(any());
        verify(cjOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("malformed event time falls back to Instant.now without throwing")
    void malformedEventTime() {
        CjLogisticWebhookParams params = sampleParams();
        params.getEvents().get(0).setEventTime("not-a-date");
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.empty());
        handler.handleLogisticEvent(params);
        verify(trackingEventRepository).save(any());
    }

    @Test
    @DisplayName("blank event time also defaults to now")
    void blankEventTime() {
        CjLogisticWebhookParams params = sampleParams();
        params.getEvents().get(0).setEventTime("  ");
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.empty());
        handler.handleLogisticEvent(params);
        verify(trackingEventRepository).save(any());
    }

    @Test
    @DisplayName("does not iterate when events list is null")
    void nullEventsList() {
        CjLogisticWebhookParams params = sampleParams();
        params.setEvents(null);
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.empty());
        handler.handleLogisticEvent(params);
        verify(trackingEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("enrichOrder leaves untouched fields whose webhook value is null")
    void enrichOrderSparseFields() {
        CjOrder cj = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").build();
        when(cjOrderRepository.findByCjTrackNumber("TRK-1")).thenReturn(Optional.of(cj));
        OrderEntity order = OrderEntity.builder().id("ord-1").trackingUrl("PRESERVED").lastMileCarrier("KEEP")
                .lastMileTrackNumber("KEEP-LM").build();
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(cjOrderRepository.findByOrderId("ord-1")).thenReturn(Optional.of(cj));

        CjLogisticWebhookParams params = sampleParams();
        params.setTrackUrl(null);
        params.setLastMileCarrier(null);
        params.setLastTrackNumber(null);
        params.setLogisticName(null);

        handler.handleLogisticEvent(params);

        // Pre-existing values must remain untouched when webhook does not include them
        assertThat(order.getTrackingUrl()).isEqualTo("PRESERVED");
        assertThat(order.getLastMileCarrier()).isEqualTo("KEEP");
        assertThat(order.getLastMileTrackNumber()).isEqualTo("KEEP-LM");
    }

    @Test
    @DisplayName("CJ-order tracking number stays untouched when webhook trackingNumber is null")
    void nullTrackingNumberOnCjOrder() {
        CjOrder cj = CjOrder.builder().orderId("ord-1").cjOrderId("cj-1").trackNumber("OLD").build();
        // findByCjTrackNumber(null) is the lookup we want to test
        when(cjOrderRepository.findByCjTrackNumber(null)).thenReturn(Optional.of(cj));
        when(orderJpaRepository.findById("ord-1")).thenReturn(Optional.of(OrderEntity.builder().id("ord-1").build()));
        when(cjOrderRepository.findByOrderId("ord-1")).thenReturn(Optional.of(cj));

        CjLogisticWebhookParams params = sampleParams();
        params.setTrackingNumber(null);

        handler.handleLogisticEvent(params);
        assertThat(cj.getTrackNumber()).isEqualTo("OLD"); // unchanged
    }
}
