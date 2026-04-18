package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.TrackingEvent;
import com.backandwhite.domain.repository.TrackingRepository;
import com.backandwhite.infrastructure.db.postgres.mapper.TrackingInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.TrackingEventJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackingRepositoryImpl implements TrackingRepository {

    private final TrackingEventJpaRepository jpa;
    private final TrackingInfraMapper mapper;

    @Override
    public TrackingEvent save(TrackingEvent event) {
        event.setId(UUID.randomUUID().toString());
        return mapper.toDomain(jpa.save(mapper.toEntity(event)));
    }

    @Override
    public List<TrackingEvent> findByOrderId(String orderId) {
        return mapper.toDomainList(jpa.findByOrderIdOrderByEventAtAsc(orderId));
    }
}
