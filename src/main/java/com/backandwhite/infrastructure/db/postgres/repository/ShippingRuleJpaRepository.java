package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.ShippingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingRuleJpaRepository
                extends JpaRepository<ShippingRuleEntity, String>, JpaSpecificationExecutor<ShippingRuleEntity> {

        @Query(value = "SELECT r.*, c.name AS carrier_name, c.code AS carrier_code, c.logo_url, c.active AS carrier_active, "
                        +
                        "c.created_at AS c_created_at, c.updated_at AS c_updated_at, c.created_by AS c_created_by, c.updated_by AS c_updated_by "
                        +
                        "FROM shipping_rules r JOIN shipping_carriers c ON r.carrier_id = c.id " +
                        "WHERE r.active = true AND c.active = true " +
                        "AND r.zone = :zone " +
                        "AND (r.min_weight IS NULL OR r.min_weight <= :weight) " +
                        "AND (r.max_weight IS NULL OR r.max_weight >= :weight) " +
                        "AND (r.min_price IS NULL OR r.min_price <= :subtotal) " +
                        "AND (r.max_price IS NULL OR r.max_price >= :subtotal)", nativeQuery = true)
        List<ShippingRuleEntity> findApplicableRules(@Param("zone") String zone,
                        @Param("weight") BigDecimal weight,
                        @Param("subtotal") BigDecimal subtotal);
}
