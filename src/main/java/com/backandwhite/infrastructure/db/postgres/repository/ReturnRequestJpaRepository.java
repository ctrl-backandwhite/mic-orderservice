package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.ReturnRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReturnRequestJpaRepository
        extends JpaRepository<ReturnRequestEntity, String>, JpaSpecificationExecutor<ReturnRequestEntity> {
    Page<ReturnRequestEntity> findByUserId(String userId, Pageable pageable);
}
