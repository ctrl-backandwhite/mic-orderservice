package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjTrackingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CjTrackingEventJpaRepository extends JpaRepository<CjTrackingEventEntity, String> {

    List<CjTrackingEventEntity> findByOrderIdOrderByEventTimeDesc(String orderId);

    List<CjTrackingEventEntity> findByTrackingNumberOrderByEventTimeDesc(String trackingNumber);
}
