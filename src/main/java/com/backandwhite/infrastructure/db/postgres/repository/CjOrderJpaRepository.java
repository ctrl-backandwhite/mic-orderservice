package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CjOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CjOrderJpaRepository extends JpaRepository<CjOrderEntity, String> {

    Optional<CjOrderEntity> findByOrderId(String orderId);

    Optional<CjOrderEntity> findByCjOrderId(String cjOrderId);

    Optional<CjOrderEntity> findByTrackNumber(String trackNumber);

    List<CjOrderEntity> findByCjOrderStatusAndErrorCountLessThan(CjOrderStatus status, int maxErrors);

    List<CjOrderEntity> findByCjOrderStatusInAndErrorCountLessThan(Collection<CjOrderStatus> statuses, int maxErrors);

    @Query("""
            SELECT c FROM CjOrderEntity c
            WHERE c.cjOrderId IS NOT NULL
              AND c.cjOrderStatus NOT IN ('SHIPPED', 'DELIVERED', 'CANCELLED')
              AND c.errorCount < 5
            ORDER BY c.lastSyncedAt ASC NULLS FIRST
            LIMIT :batchSize
            """)
    List<CjOrderEntity> findPendingSync(@Param("batchSize") int batchSize);
}
