package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.TrackingEvent;

import java.util.List;

public interface TrackingUseCase {
    TrackingEvent addEvent(TrackingEvent event);

    List<TrackingEvent> findByOrderId(String orderId);
}
