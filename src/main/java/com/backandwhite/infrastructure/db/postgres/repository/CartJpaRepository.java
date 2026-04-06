package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.domain.valueobject.CartStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, String> {
    Optional<CartEntity> findByUserIdAndStatus(String userId, CartStatus status);

    Optional<CartEntity> findBySessionIdAndStatus(String sessionId, CartStatus status);
}
