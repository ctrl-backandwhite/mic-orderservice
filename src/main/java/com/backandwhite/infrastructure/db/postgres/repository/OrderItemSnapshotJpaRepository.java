package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderItemSnapshotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemSnapshotJpaRepository extends JpaRepository<OrderItemSnapshotEntity, Long> {

    List<OrderItemSnapshotEntity> findAllByOrderIdOrderByIdAsc(String orderId);
}
