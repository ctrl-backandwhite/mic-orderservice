package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjDisputeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjDisputeJpaRepository extends JpaRepository<CjDisputeEntity, String> {

    List<CjDisputeEntity> findAllByOrderIdOrderByCreatedAtDesc(String orderId);

    List<CjDisputeEntity> findAllByStatusOrderByCreatedAtDesc(String status);
}
