package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.InvoiceEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceJpaRepository
        extends
            JpaRepository<InvoiceEntity, String>,
            JpaSpecificationExecutor<InvoiceEntity> {
    Optional<InvoiceEntity> findByOrderId(String orderId);

    @Query("SELECT i FROM InvoiceEntity i JOIN i.order o WHERE o.userId = :userId")
    Page<InvoiceEntity> findByUserId(String userId, Pageable pageable);
}
