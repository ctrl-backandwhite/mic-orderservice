package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjAllowedCountryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjAllowedCountryJpaRepository extends JpaRepository<CjAllowedCountryEntity, String> {

    List<CjAllowedCountryEntity> findAllByActiveTrueOrderByCountryCodeAsc();

    boolean existsByCountryCodeAndActiveTrue(String countryCode);
}
