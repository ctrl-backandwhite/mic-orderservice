package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.ShippingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingRuleJpaRepository
        extends JpaRepository<ShippingRuleEntity, String>, JpaSpecificationExecutor<ShippingRuleEntity> {

    @Query("SELECT r FROM ShippingRuleEntity r JOIN FETCH r.carrier c " +
            "WHERE r.active = true AND c.active = true " +
            "AND r.zone = :zone " +
            "AND (r.minWeight IS NULL OR r.minWeight <= :weight) " +
            "AND (r.maxWeight IS NULL OR r.maxWeight >= :weight) " +
            "AND (r.minPrice IS NULL OR r.minPrice <= :subtotal) " +
            "AND (r.maxPrice IS NULL OR r.maxPrice >= :subtotal)")
    List<ShippingRuleEntity> findApplicableRules(String zone, BigDecimal weight, BigDecimal subtotal);
}
