package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.ShippingCarrierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShippingCarrierJpaRepository
        extends JpaRepository<ShippingCarrierEntity, String>, JpaSpecificationExecutor<ShippingCarrierEntity> {
}
