package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjApiAuditLogEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CjApiAuditLogJpaRepository extends JpaRepository<CjApiAuditLogEntity, Long> {

    List<CjApiAuditLogEntity> findAllByOrderIdOrderByCreatedAtAsc(String orderId);

    @Modifying
    @Query("DELETE FROM CjApiAuditLogEntity a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(Instant cutoff);
}
