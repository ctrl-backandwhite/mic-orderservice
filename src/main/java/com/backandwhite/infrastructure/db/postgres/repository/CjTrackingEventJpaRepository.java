package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjTrackingEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjTrackingEventJpaRepository extends JpaRepository<CjTrackingEventEntity, String> {

    List<CjTrackingEventEntity> findByOrderIdOrderByEventTimeDesc(String orderId);

    List<CjTrackingEventEntity> findByTrackingNumberOrderByEventTimeDesc(String trackingNumber);
}
