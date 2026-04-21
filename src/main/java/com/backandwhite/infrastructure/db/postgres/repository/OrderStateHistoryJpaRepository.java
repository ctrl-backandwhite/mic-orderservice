package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.OrderStateHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStateHistoryJpaRepository extends JpaRepository<OrderStateHistoryEntity, Long> {

    List<OrderStateHistoryEntity> findAllByOrderIdOrderByAtAsc(String orderId);
}
