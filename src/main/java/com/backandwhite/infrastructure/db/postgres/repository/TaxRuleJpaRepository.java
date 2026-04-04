package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.TaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaxRuleJpaRepository
        extends JpaRepository<TaxRuleEntity, String>, JpaSpecificationExecutor<TaxRuleEntity> {
    List<TaxRuleEntity> findByCountryAndActiveTrue(String country);

    List<TaxRuleEntity> findByCountryAndRegionAndActiveTrue(String country, String region);
}
