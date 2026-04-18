package com.backandwhite.application.usecase.impl;

import com.backandwhite.application.usecase.CartUseCase;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.valueobject.CartStatus;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class CartUseCaseImpl implements CartUseCase {

    private final CartRepository cartRepository;

    @Override
    @Transactional(readOnly = true)
    public Cart getActiveCart(String userId, String sessionId) {
        Optional<Cart> cart = (userId != null)
                ? cartRepository.findActiveByUserId(userId)
                : cartRepository.findActiveBySessionId(sessionId);
        return cart.orElse(Cart.builder().items(new ArrayList<>()).subtotal(Money.zero()).itemCount(0).build());
    }

    @Override
    @Transactional
    public Cart getOrCreateCart(String userId, String sessionId) {
        Optional<Cart> existing = (userId != null)
                ? cartRepository.findActiveByUserId(userId)
                : cartRepository.findActiveBySessionId(sessionId);

        return existing.orElseGet(() -> {
            Cart newCart = Cart.builder().userId(userId).sessionId(sessionId).status(CartStatus.ACTIVE)
                    .items(new ArrayList<>()).build();
            return cartRepository.save(newCart);
        });
    }

    @Override
    @Transactional
    public Cart addItem(String userId, String sessionId, CartItem item) {
        Cart cart = getOrCreateCart(userId, sessionId);

        Optional<CartItem> existing = cartRepository.findItemByCartAndProduct(cart.getId(), item.getProductId(),
                item.getVariantId());

        if (existing.isPresent()) {
            CartItem existingItem = existing.get();
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            cartRepository.updateItem(existingItem);
        } else {
            item.setCartId(cart.getId());
            cartRepository.addItem(cart.getId(), item);
        }

        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Override
    @Transactional
    public Cart updateItemQuantity(String itemId, int quantity) {
        CartItem item = cartRepository.findItemById(itemId)
                .orElseThrow(() -> new com.backandwhite.common.exception.EntityNotFoundException("NF001",
                        "CartItem with id " + itemId + " is not found."));
        item.setQuantity(quantity);
        cartRepository.updateItem(item);
        return cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new com.backandwhite.common.exception.EntityNotFoundException("NF001",
                        "Cart not found for item " + itemId));
    }

    @Override
    @Transactional
    public Cart removeItem(String userId, String sessionId, String itemId) {
        CartItem item = cartRepository.findItemById(itemId)
                .orElseThrow(() -> new com.backandwhite.common.exception.EntityNotFoundException("NF001",
                        "CartItem with id " + itemId + " is not found."));
        String cartId = item.getCartId();
        cartRepository.removeItem(itemId);
        return cartRepository.findById(cartId)
                .orElse(Cart.builder().items(new ArrayList<>()).subtotal(Money.zero()).itemCount(0).build());
    }

    @Override
    @Transactional
    public void clearCart(String userId, String sessionId) {
        Optional<Cart> cart = (userId != null)
                ? cartRepository.findActiveByUserId(userId)
                : cartRepository.findActiveBySessionId(sessionId);
        cart.ifPresent(c -> cartRepository.clearItems(c.getId()));
    }

    @Override
    @Transactional
    public Cart mergeCart(String userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return getActiveCart(userId, sessionId);
        }

        Optional<Cart> anonCart = cartRepository.findActiveBySessionId(sessionId);
        Optional<Cart> userCart = cartRepository.findActiveByUserId(userId);

        if (anonCart.isEmpty()) {
            return userCart.orElse(Cart.builder().items(new ArrayList<>()).subtotal(Money.zero()).itemCount(0).build());
        }

        Cart target = userCart.orElseGet(() -> {
            Cart newCart = Cart.builder().userId(userId).status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
            return cartRepository.save(newCart);
        });

        Cart anon = anonCart.get();
        if (anon.getItems() != null) {
            for (CartItem anonItem : anon.getItems()) {
                Optional<CartItem> existing = cartRepository.findItemByCartAndProduct(target.getId(),
                        anonItem.getProductId(), anonItem.getVariantId());
                if (existing.isPresent()) {
                    CartItem ex = existing.get();
                    ex.setQuantity(ex.getQuantity() + anonItem.getQuantity());
                    cartRepository.updateItem(ex);
                } else {
                    anonItem.setCartId(target.getId());
                    anonItem.setId(null);
                    cartRepository.addItem(target.getId(), anonItem);
                }
            }
        }

        anon.setStatus(CartStatus.MERGED);
        cartRepository.update(anon);

        return cartRepository.findActiveByUserId(userId).orElse(target);
    }
}
