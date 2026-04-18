package com.backandwhite.application.usecase.impl;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;

import com.backandwhite.application.usecase.TrackingUseCase;
import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.repository.TrackingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackingUseCaseImpl implements TrackingUseCase {

    private final TrackingRepository trackingRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public TrackingEvent addEvent(TrackingEvent event) {
        orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", event.getOrderId()));
        return trackingRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEvent> findByOrderId(String orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", orderId));
        return trackingRepository.findByOrderId(orderId);
    }
}
