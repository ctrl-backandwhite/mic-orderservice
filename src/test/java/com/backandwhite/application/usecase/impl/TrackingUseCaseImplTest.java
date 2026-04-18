package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.repository.TrackingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingUseCaseImplTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TrackingUseCaseImpl useCase;

    @Test
    void addEvent_orderExists_savesEvent() {
        TrackingEvent event = TrackingEvent.builder().orderId("o1").status("SHIPPED").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(Order.builder().id("o1").build()));
        when(trackingRepository.save(event)).thenReturn(event);

        TrackingEvent result = useCase.addEvent(event);

        assertThat(result).isSameAs(event);
    }

    @Test
    void addEvent_orderMissing_throws() {
        TrackingEvent event = TrackingEvent.builder().orderId("missing").build();
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.addEvent(event)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByOrderId_orderExists_returnsEvents() {
        when(orderRepository.findById("o1")).thenReturn(Optional.of(Order.builder().id("o1").build()));
        List<TrackingEvent> events = List.of(TrackingEvent.builder().id("e1").build());
        when(trackingRepository.findByOrderId("o1")).thenReturn(events);

        assertThat(useCase.findByOrderId("o1")).hasSize(1);
    }

    @Test
    void findByOrderId_missingOrder_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findByOrderId("x")).isInstanceOf(EntityNotFoundException.class);
    }
}
