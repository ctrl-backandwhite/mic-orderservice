package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.TrackingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventJpaRepository extends JpaRepository<TrackingEventEntity, String> {
    List<TrackingEventEntity> findByOrderIdOrderByEventAtAsc(String orderId);
}
