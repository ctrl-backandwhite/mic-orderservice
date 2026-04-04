package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryJpaRepository extends JpaRepository<OrderStatusHistoryEntity, String> {
    List<OrderStatusHistoryEntity> findByOrderIdOrderByChangedAtAsc(String orderId);
}
