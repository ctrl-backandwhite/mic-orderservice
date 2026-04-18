package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.TrackingEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventJpaRepository extends JpaRepository<TrackingEventEntity, String> {
    List<TrackingEventEntity> findByOrderIdOrderByEventAtAsc(String orderId);
}
