package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.valueobject.CartStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartUseCaseImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartUseCaseImpl useCase;

    @Test
    void getActiveCart_byUser_returnsExisting() {
        Cart c = Cart.builder().id("c1").items(new ArrayList<>()).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(c));
        assertThat(useCase.getActiveCart("u1", null)).isSameAs(c);
    }

    @Test
    void getActiveCart_bySession_returnsExisting() {
        Cart c = Cart.builder().id("c1").build();
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(c));
        assertThat(useCase.getActiveCart(null, "s1")).isSameAs(c);
    }

    @Test
    void getActiveCart_none_returnsEmpty() {
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        Cart result = useCase.getActiveCart("u1", null);
        assertThat(result.getId()).isNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void getOrCreateCart_existingForUser_returns() {
        Cart c = Cart.builder().id("c1").build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(c));
        assertThat(useCase.getOrCreateCart("u1", null)).isSameAs(c);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_noneExists_createsNew() {
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        Cart newCart = Cart.builder().id("new").build();
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = useCase.getOrCreateCart("u1", null);
        assertThat(result).isSameAs(newCart);
    }

    @Test
    void getOrCreateCart_anonymousSession_createsNew() {
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.empty());
        Cart newCart = Cart.builder().id("new").build();
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = useCase.getOrCreateCart(null, "s1");
        assertThat(result).isSameAs(newCart);
    }

    @Test
    void addItem_existingItem_updatesQuantity() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        CartItem newItem = CartItem.builder().productId("p1").variantId("v1").quantity(3).build();
        CartItem existing = CartItem.builder().id("i1").cartId("c1").productId("p1").variantId("v1").quantity(2)
                .build();
        when(cartRepository.findItemByCartAndProduct("c1", "p1", "v1")).thenReturn(Optional.of(existing));
        when(cartRepository.findById("c1")).thenReturn(Optional.of(cart));

        useCase.addItem("u1", null, newItem);
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartRepository).updateItem(existing);
    }

    @Test
    void addItem_newItem_adds() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        CartItem newItem = CartItem.builder().productId("p1").variantId("v1").quantity(3).build();
        when(cartRepository.findItemByCartAndProduct(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(cartRepository.findById("c1")).thenReturn(Optional.of(cart));

        useCase.addItem("u1", null, newItem);
        verify(cartRepository).addItem(eq("c1"), eq(newItem));
        assertThat(newItem.getCartId()).isEqualTo("c1");
    }

    @Test
    void addItem_findByIdReturnsEmpty_fallbackToCart() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        CartItem newItem = CartItem.builder().productId("p1").variantId("v1").quantity(1).build();
        when(cartRepository.findItemByCartAndProduct(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(cartRepository.findById("c1")).thenReturn(Optional.empty());

        Cart result = useCase.addItem("u1", null, newItem);
        assertThat(result).isSameAs(cart);
    }

    @Test
    void updateItemQuantity_existing_updates() {
        CartItem item = CartItem.builder().id("i1").cartId("c1").quantity(2).build();
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findItemById("i1")).thenReturn(Optional.of(item));
        when(cartRepository.findById("c1")).thenReturn(Optional.of(cart));

        Cart result = useCase.updateItemQuantity("i1", 7);
        assertThat(item.getQuantity()).isEqualTo(7);
        assertThat(result).isSameAs(cart);
    }

    @Test
    void updateItemQuantity_missingItem_throws() {
        when(cartRepository.findItemById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateItemQuantity("x", 1)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateItemQuantity_missingCart_throws() {
        CartItem item = CartItem.builder().id("i1").cartId("missing").quantity(1).build();
        when(cartRepository.findItemById("i1")).thenReturn(Optional.of(item));
        when(cartRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateItemQuantity("i1", 5)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void removeItem_existing_removes() {
        CartItem item = CartItem.builder().id("i1").cartId("c1").build();
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findItemById("i1")).thenReturn(Optional.of(item));
        when(cartRepository.findById("c1")).thenReturn(Optional.of(cart));

        Cart result = useCase.removeItem("u1", null, "i1");
        verify(cartRepository).removeItem("i1");
        assertThat(result).isSameAs(cart);
    }

    @Test
    void removeItem_cartNotFoundAfter_returnsEmpty() {
        CartItem item = CartItem.builder().id("i1").cartId("c1").build();
        when(cartRepository.findItemById("i1")).thenReturn(Optional.of(item));
        when(cartRepository.findById("c1")).thenReturn(Optional.empty());

        Cart result = useCase.removeItem("u1", null, "i1");
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void removeItem_missingItem_throws() {
        when(cartRepository.findItemById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.removeItem("u1", null, "x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void clearCart_withActiveUserCart_clears() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        useCase.clearCart("u1", null);
        verify(cartRepository).clearItems("c1");
    }

    @Test
    void clearCart_noCart_doesNothing() {
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.empty());
        useCase.clearCart(null, "s1");
        verify(cartRepository, never()).clearItems(anyString());
    }

    @Test
    void mergeCart_missingUserId_returnsActive() {
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.empty());
        Cart result = useCase.mergeCart(null, "s1");
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void mergeCart_missingSessionId_returnsActive() {
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        Cart result = useCase.mergeCart("u1", null);
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void mergeCart_noAnonCart_returnsUserCart() {
        Cart userCart = Cart.builder().id("u-cart").build();
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.empty());
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(userCart));

        Cart result = useCase.mergeCart("u1", "s1");
        assertThat(result).isSameAs(userCart);
    }

    @Test
    void mergeCart_noAnonNoUser_returnsEmpty() {
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.empty());
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());

        Cart result = useCase.mergeCart("u1", "s1");
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void mergeCart_anonWithItems_mergesToExistingUserCart() {
        CartItem anonItem1 = CartItem.builder().id("i1").productId("p1").variantId("v1").quantity(1).build();
        CartItem anonItem2 = CartItem.builder().id("i2").productId("p2").variantId("v2").quantity(3).build();
        Cart anon = Cart.builder().id("anon").status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(anonItem1, anonItem2))).build();
        Cart userCart = Cart.builder().id("user").build();

        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(anon));
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(userCart), Optional.of(userCart));

        CartItem existing = CartItem.builder().id("eid").cartId("user").productId("p1").variantId("v1").quantity(2)
                .build();
        when(cartRepository.findItemByCartAndProduct("user", "p1", "v1")).thenReturn(Optional.of(existing));
        when(cartRepository.findItemByCartAndProduct("user", "p2", "v2")).thenReturn(Optional.empty());

        Cart result = useCase.mergeCart("u1", "s1");
        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartRepository).addItem(eq("user"), eq(anonItem2));
        assertThat(anon.getStatus()).isEqualTo(CartStatus.MERGED);
        verify(cartRepository).update(anon);
        assertThat(result).isSameAs(userCart);
    }

    @Test
    void mergeCart_anonItemsNull_stillMarksMerged() {
        Cart anon = Cart.builder().id("anon").status(CartStatus.ACTIVE).items(null).build();
        Cart userCart = Cart.builder().id("user").build();
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(anon));
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(userCart), Optional.of(userCart));

        useCase.mergeCart("u1", "s1");
        assertThat(anon.getStatus()).isEqualTo(CartStatus.MERGED);
    }

    @Test
    void mergeCart_anonWithItems_noUserCart_createsNewUserCart() {
        CartItem anonItem = CartItem.builder().id("i1").productId("p1").variantId("v1").quantity(1).build();
        Cart anon = Cart.builder().id("anon").status(CartStatus.ACTIVE).items(new ArrayList<>(List.of(anonItem)))
                .build();
        Cart newUserCart = Cart.builder().id("newUser").build();

        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(anon));
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty(), Optional.of(newUserCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(newUserCart);
        when(cartRepository.findItemByCartAndProduct("newUser", "p1", "v1")).thenReturn(Optional.empty());

        Cart result = useCase.mergeCart("u1", "s1");
        assertThat(result).isSameAs(newUserCart);
    }

    @Test
    void mergeCart_userCartFallbackWhenActiveMissing_returnsTarget() {
        Cart anon = Cart.builder().id("anon").status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
        Cart userCart = Cart.builder().id("user").build();
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(anon));
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(userCart), Optional.empty());

        Cart result = useCase.mergeCart("u1", "s1");
        assertThat(result).isSameAs(userCart);
    }
}
