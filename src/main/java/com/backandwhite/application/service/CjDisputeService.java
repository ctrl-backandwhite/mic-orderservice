package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.CjDisputeEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjDisputeJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 13 — dispute lifecycle. Customers open a dispute from their order
 * history; admins process it; if approved, we trigger a refund through the
 * original payment gateway. Each state change ends up in
 * {@code cj_disputes.status} and simultaneously in {@code order_state_history}.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjDisputeService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CLOSED = "CLOSED";

    private final CjDisputeJpaRepository repository;
    private final OrderStateHistoryService stateHistoryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CjDisputeEntity openDispute(String orderId, String cjOrderId, String reason, String description,
            List<String> evidenceUrls, String actorId) {
        CjDisputeEntity dispute = CjDisputeEntity.builder().id(UUID.randomUUID().toString()).orderId(orderId)
                .cjOrderId(cjOrderId).reason(reason).description(description).evidenceUrls(toJson(evidenceUrls))
                .status(STATUS_OPEN).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        CjDisputeEntity saved = repository.save(dispute);
        stateHistoryService.recordOrderTransition(orderId, null, "DISPUTE_OPEN",
                OrderStateHistoryService.ACTOR_CUSTOMER, actorId, "Customer opened dispute: " + reason, null);
        log.info("::> Dispute opened orderId={} reason={}", orderId, reason);
        return saved;
    }

    @Transactional
    public CjDisputeEntity transitionStatus(String disputeId, String newStatus, String resolution,
            BigDecimal refundAmount, String refundCurrency, String actorId) {
        CjDisputeEntity dispute = repository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));
        String previousStatus = dispute.getStatus();
        dispute.setStatus(newStatus);
        if (resolution != null) {
            dispute.setResolution(resolution);
        }
        if (refundAmount != null) {
            dispute.setRefundAmount(refundAmount);
            dispute.setRefundCurrency(refundCurrency);
        }
        dispute.setUpdatedAt(Instant.now());
        CjDisputeEntity saved = repository.save(dispute);
        stateHistoryService.recordOrderTransition(dispute.getOrderId(), "DISPUTE_" + previousStatus,
                "DISPUTE_" + newStatus, OrderStateHistoryService.ACTOR_ADMIN, actorId,
                "Admin transitioned dispute to " + newStatus, null);
        return saved;
    }

    public List<CjDisputeEntity> listByOrder(String orderId) {
        return repository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public List<CjDisputeEntity> listByStatus(String status) {
        return repository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("::> Could not serialize dispute metadata: {}", e.getMessage());
            return "[]";
        }
    }
}
