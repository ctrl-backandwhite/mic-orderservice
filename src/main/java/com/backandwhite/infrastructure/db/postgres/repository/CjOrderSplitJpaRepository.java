package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjOrderSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CjOrderSplitJpaRepository extends JpaRepository<CjOrderSplitEntity, String> {

    List<CjOrderSplitEntity> findByOrderId(String orderId);

    List<CjOrderSplitEntity> findByOriginalCjOrderId(String originalCjOrderId);
}
