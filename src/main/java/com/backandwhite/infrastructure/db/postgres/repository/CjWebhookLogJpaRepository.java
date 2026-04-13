package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CjWebhookLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CjWebhookLogJpaRepository extends JpaRepository<CjWebhookLogEntity, String> {

    boolean existsByMessageId(String messageId);
}
