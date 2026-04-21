package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.AdminPiiAccessLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AdminPiiAccessLogJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 15 — records every time an admin reads or exports customer PII, so we
 * can answer compliance subject-access requests ("who looked at my data?").
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AdminPiiAuditService {

    public static final String ACTION_VIEW_ADDRESS = "VIEW_ADDRESS";
    public static final String ACTION_VIEW_PHONE = "VIEW_PHONE";
    public static final String ACTION_VIEW_EMAIL = "VIEW_EMAIL";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_DELETE_REQUEST = "DELETE_REQUEST";

    private final AdminPiiAccessLogJpaRepository repository;

    @Transactional
    public void record(String adminUserId, String customerId, String orderId, String action, String reason,
            String sourceIp) {
        AdminPiiAccessLogEntity entity = AdminPiiAccessLogEntity.builder().at(Instant.now()).adminUserId(adminUserId)
                .customerId(customerId).orderId(orderId).action(action).reason(reason).sourceIp(sourceIp).build();
        repository.save(entity);
        log.info("::> PII access: admin={} action={} customer={} order={}", adminUserId, action, customerId, orderId);
    }

    public List<AdminPiiAccessLogEntity> listByCustomer(String customerId) {
        return repository.findAllByCustomerIdOrderByAtDesc(customerId);
    }
}
