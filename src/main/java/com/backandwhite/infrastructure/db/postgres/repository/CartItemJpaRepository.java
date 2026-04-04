package com.backandwhite.infrastructure.db.postgres.repository;

import com.backandwhite.infrastructure.db.postgres.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, String> {
    List<CartItemEntity> findByCartId(String cartId);

    Optional<CartItemEntity> findByCartIdAndProductIdAndVariantId(String cartId, String productId, String variantId);

    void deleteByCartId(String cartId);
}
