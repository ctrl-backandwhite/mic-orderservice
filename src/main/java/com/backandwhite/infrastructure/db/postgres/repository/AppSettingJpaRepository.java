package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.AppSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingJpaRepository extends JpaRepository<AppSettingEntity, String> {
}
