package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.CjOrderStateHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStateHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjOrderStateHistoryJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderStateHistoryJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 8.2 — append-only event log of every status transition on local orders
 * and their CJ counterparts. Lets support rebuild the timeline of any order
 * without consulting the destructive {@code orders.status} column.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class OrderStateHistoryService {

    public static final String ACTOR_SYSTEM = "SYSTEM";
    public static final String ACTOR_WEBHOOK = "WEBHOOK";
    public static final String ACTOR_ADMIN = "ADMIN";
    public static final String ACTOR_CUSTOMER = "CUSTOMER";
    public static final String ACTOR_SCHEDULER = "SCHEDULER";

    private final OrderStateHistoryJpaRepository orderHistoryRepository;
    private final CjOrderStateHistoryJpaRepository cjHistoryRepository;

    @Transactional
    public OrderStateHistoryEntity recordOrderTransition(String orderId, String fromStatus, String toStatus,
            String actor, String actorId, String reason, String metadata) {
        OrderStateHistoryEntity entity = OrderStateHistoryEntity.builder().orderId(orderId).at(Instant.now())
                .fromStatus(fromStatus).toStatus(toStatus).actor(actor).actorId(actorId).reason(reason)
                .metadata(metadata).build();
        log.info("::> Order state transition orderId={} {}→{} actor={}", orderId, fromStatus, toStatus, actor);
        return orderHistoryRepository.save(entity);
    }

    @Transactional
    public CjOrderStateHistoryEntity recordCjTransition(String cjOrderId, String orderId, String fromStatus,
            String toStatus, String actor, String reason, String metadata) {
        CjOrderStateHistoryEntity entity = CjOrderStateHistoryEntity.builder().cjOrderId(cjOrderId).orderId(orderId)
                .at(Instant.now()).fromStatus(fromStatus).toStatus(toStatus).actor(actor).reason(reason)
                .metadata(metadata).build();
        log.info("::> CJ state transition cjOrderId={} {}→{} actor={}", cjOrderId, fromStatus, toStatus, actor);
        return cjHistoryRepository.save(entity);
    }

    public List<OrderStateHistoryEntity> findOrderHistory(String orderId) {
        return orderHistoryRepository.findAllByOrderIdOrderByAtAsc(orderId);
    }

    public List<CjOrderStateHistoryEntity> findCjHistoryByOrder(String orderId) {
        return cjHistoryRepository.findAllByOrderIdOrderByAtAsc(orderId);
    }

    public List<CjOrderStateHistoryEntity> findCjHistoryByCjOrder(String cjOrderId) {
        return cjHistoryRepository.findAllByCjOrderIdOrderByAtAsc(cjOrderId);
    }
}
