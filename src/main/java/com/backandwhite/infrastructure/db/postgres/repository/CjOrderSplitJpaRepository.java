package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjOrderSplitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjOrderSplitJpaRepository extends JpaRepository<CjOrderSplitEntity, String> {

    List<CjOrderSplitEntity> findByOrderId(String orderId);

    List<CjOrderSplitEntity> findByOriginalCjOrderId(String originalCjOrderId);
}
