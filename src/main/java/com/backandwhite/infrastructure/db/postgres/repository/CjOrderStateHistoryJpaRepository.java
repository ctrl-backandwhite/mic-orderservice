package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjOrderStateHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjOrderStateHistoryJpaRepository extends JpaRepository<CjOrderStateHistoryEntity, Long> {

    List<CjOrderStateHistoryEntity> findAllByOrderIdOrderByAtAsc(String orderId);

    List<CjOrderStateHistoryEntity> findAllByCjOrderIdOrderByAtAsc(String cjOrderId);
}
