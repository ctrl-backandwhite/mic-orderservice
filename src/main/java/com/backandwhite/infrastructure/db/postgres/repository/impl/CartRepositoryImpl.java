package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.valureobject.CartStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CartEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CartItemEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.CartInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CartItemJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpa;
    private final CartItemJpaRepository cartItemJpa;
    private final CartInfraMapper mapper;

    @Override
    public Optional<Cart> findActiveByUserId(String userId) {
        return cartJpa.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .map(this::enrichCart);
    }

    @Override
    public Optional<Cart> findActiveBySessionId(String sessionId) {
        return cartJpa.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                .map(this::enrichCart);
    }

    @Override
    public Optional<Cart> findById(String id) {
        return cartJpa.findById(id).map(this::enrichCart);
    }

    @Override
    public Cart save(Cart cart) {
        cart.setId(UUID.randomUUID().toString());
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        CartEntity entity = mapper.toEntity(cart);
        return enrichCart(cartJpa.save(entity));
    }

    @Override
    public Cart update(Cart cart) {
        CartEntity entity = mapper.toEntity(cart);
        return enrichCart(cartJpa.save(entity));
    }

    @Override
    public void delete(String id) {
        cartJpa.deleteById(id);
    }

    @Override
    public CartItem addItem(String cartId, CartItem item) {
        item.setId(UUID.randomUUID().toString());
        CartItemEntity entity = mapper.toItemEntity(item);
        entity.setCart(cartJpa.getReferenceById(cartId));
        return mapper.toItemDomain(cartItemJpa.save(entity));
    }

    @Override
    public CartItem updateItem(CartItem item) {
        CartItemEntity entity = mapper.toItemEntity(item);
        entity.setCart(cartJpa.getReferenceById(item.getCartId()));
        return mapper.toItemDomain(cartItemJpa.save(entity));
    }

    @Override
    public void removeItem(String itemId) {
        cartItemJpa.deleteById(itemId);
    }

    @Override
    public void clearItems(String cartId) {
        cartItemJpa.deleteByCartId(cartId);
    }

    @Override
    public Optional<CartItem> findItemByCartAndProduct(String cartId, String productId, String variantId) {
        return cartItemJpa.findByCartIdAndProductIdAndVariantId(cartId, productId, variantId)
                .map(mapper::toItemDomain);
    }

    @Override
    public Optional<CartItem> findItemById(String itemId) {
        return cartItemJpa.findById(itemId).map(mapper::toItemDomain);
    }

    private Cart enrichCart(CartEntity entity) {
        Cart cart = mapper.toDomain(entity);
        var items = cart.getItems() != null ? cart.getItems() : java.util.List.<CartItem>of();
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItem::getQuantity).sum();
        cart.setSubtotal(subtotal);
        cart.setItemCount(count);
        return cart;
    }
}
