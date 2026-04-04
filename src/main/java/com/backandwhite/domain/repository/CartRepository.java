package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;

import java.util.Optional;

public interface CartRepository {
    Optional<Cart> findActiveByUserId(String userId);

    Optional<Cart> findActiveBySessionId(String sessionId);

    Optional<Cart> findById(String id);

    Cart save(Cart cart);

    Cart update(Cart cart);

    void delete(String id);

    CartItem addItem(String cartId, CartItem item);

    CartItem updateItem(CartItem item);

    void removeItem(String itemId);

    void clearItems(String cartId);

    Optional<CartItem> findItemByCartAndProduct(String cartId, String productId, String variantId);

    Optional<CartItem> findItemById(String itemId);
}
