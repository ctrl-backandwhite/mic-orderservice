package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CouponUsageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageJpaRepository extends JpaRepository<CouponUsageEntity, String> {
    int countByCouponIdAndUserId(String couponId, String userId);

    List<CouponUsageEntity> findByCouponIdOrderByUsedAtDesc(String couponId);
}
