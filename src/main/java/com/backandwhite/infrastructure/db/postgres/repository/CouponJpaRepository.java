package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponJpaRepository
        extends JpaRepository<CouponEntity, String>, JpaSpecificationExecutor<CouponEntity> {
    Optional<CouponEntity> findByCode(String code);

    @Modifying
    @Query("UPDATE CouponEntity c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id")
    void incrementUsedCount(String id);
}
