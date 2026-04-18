package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.TaxRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaxRuleJpaRepository
        extends
            JpaRepository<TaxRuleEntity, String>,
            JpaSpecificationExecutor<TaxRuleEntity> {
    List<TaxRuleEntity> findByCountryAndActiveTrue(String country);

    List<TaxRuleEntity> findByCountryAndRegionAndActiveTrue(String country, String region);
}
