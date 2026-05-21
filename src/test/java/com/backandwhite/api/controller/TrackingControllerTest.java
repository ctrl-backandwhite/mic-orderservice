package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.TrackingEventDtoIn;
import com.backandwhite.api.dto.out.TrackingEventDtoOut;
import com.backandwhite.api.mapper.TrackingApiMapper;
import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.domain.repository.CjOrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    @Mock
    private TrackingUseCase trackingUseCase;
    @Mock
    private TrackingApiMapper trackingApiMapper;
    @Mock
    private CjShoppingPort cjShoppingPort;
    @Mock
    private CjOrderRepository cjOrderRepository;

    @InjectMocks
    private TrackingController controller;

    @Test
    void getTrackingByOrder_returnsOk() {
        when(trackingUseCase.findByOrderId("o1")).thenReturn(List.of());
        when(trackingApiMapper.toDtoList(any())).thenReturn(List.<TrackingEventDtoOut>of());
        var resp = controller.getTrackingByOrder("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getCjTracking_orderNotFound_returnsMessage() {
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.empty());
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("message", "CJ order not found for orderId: o1");
    }

    @Test
    void getCjTracking_noTrackNumber_returnsHasNotShippedMessage() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").trackNumber("").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getBody()).containsEntry("message", "Order has not been shipped yet");
    }

    @Test
    void getCjTracking_nullTrackNumber_returnsHasNotShippedMessage() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").trackNumber(null).build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getBody()).containsEntry("message", "Order has not been shipped yet");
    }

    @Test
    void getCjTracking_validTrackInfo_returnsAllFields() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").trackNumber("TRK1").build();
        CjTrackInfo info = CjTrackInfo.builder().trackingNumber("TRK1").logisticName("DHL").trackingStatus("SHIPPED")
                .build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        when(cjShoppingPort.getTrackInfo("TRK1")).thenReturn(info);
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getBody()).containsEntry("trackingNumber", "TRK1").containsEntry("logisticName", "DHL")
                .containsEntry("trackingStatus", "SHIPPED");
    }

    @Test
    void getCjTracking_nullInfo_returnsNoDataMessage() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").trackNumber("TRK1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        when(cjShoppingPort.getTrackInfo("TRK1")).thenReturn(null);
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getBody()).containsEntry("message", "No tracking data available yet");
    }

    @Test
    void getCjTracking_throwsException_returnsTemporaryUnavailable() {
        CjOrder cjOrder = CjOrder.builder().id("c1").orderId("o1").trackNumber("TRK1").build();
        when(cjOrderRepository.findByOrderId("o1")).thenReturn(Optional.of(cjOrder));
        when(cjShoppingPort.getTrackInfo("TRK1")).thenThrow(new RuntimeException("boom"));
        var resp = controller.getCjTracking("auth", "o1");
        assertThat(resp.getBody()).containsEntry("message", "Tracking lookup temporary unavailable");
    }

    @Test
    void addEvent_returnsCreated() {
        TrackingEventDtoIn in = TrackingEventDtoIn.builder().orderId("o1").status("IN_TRANSIT").eventAt(Instant.now())
                .build();
        TrackingEvent domain = TrackingEvent.builder().build();
        TrackingEvent saved = TrackingEvent.builder().id("e1").build();
        when(trackingApiMapper.toDomain(in)).thenReturn(domain);
        when(trackingUseCase.addEvent(domain)).thenReturn(saved);
        when(trackingApiMapper.toDto(saved)).thenReturn(TrackingEventDtoOut.builder().build());
        var resp = controller.addEvent("auth", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }
}
