package com.backandwhite.application.service;

import com.backandwhite.infrastructure.db.postgres.entity.OrderItemSnapshotEntity;
import com.backandwhite.infrastructure.db.postgres.repository.OrderItemSnapshotJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 8.3 — writes an immutable record of each line item at the moment the
 * customer paid, so the order view keeps showing what the customer saw even
 * after CJ changes a product's name, image, price or stock.
 */
@Service
@RequiredArgsConstructor
public class OrderItemSnapshotService {

    private final OrderItemSnapshotJpaRepository repository;

    @Transactional
    public OrderItemSnapshotEntity save(OrderItemSnapshotEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity);
    }

    public List<OrderItemSnapshotEntity> findByOrder(String orderId) {
        return repository.findAllByOrderIdOrderByIdAsc(orderId);
    }
}
