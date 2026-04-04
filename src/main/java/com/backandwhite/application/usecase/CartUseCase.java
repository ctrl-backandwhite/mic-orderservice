package com.backandwhite.application.usecase;

import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;

public interface CartUseCase {
    Cart getActiveCart(String userId, String sessionId);

    Cart getOrCreateCart(String userId, String sessionId);

    CartItem addItem(String userId, String sessionId, CartItem item);

    CartItem updateItemQuantity(String itemId, int quantity);

    void removeItem(String itemId);

    void clearCart(String userId, String sessionId);

    Cart mergeCart(String userId, String sessionId);
}
