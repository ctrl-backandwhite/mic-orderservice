package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.TrackingEvent;
import java.util.List;

public interface TrackingRepository {
    TrackingEvent save(TrackingEvent event);

    List<TrackingEvent> findByOrderId(String orderId);
}
