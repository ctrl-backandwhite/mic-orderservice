package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.in.CartItemDtoIn;
import com.backandwhite.api.dto.in.CartItemQuantityDtoIn;
import com.backandwhite.api.dto.in.CartMergeDtoIn;
import com.backandwhite.api.dto.out.CartDtoOut;
import com.backandwhite.api.mapper.CartApiMapper;
import com.backandwhite.application.usecase.CartUseCase;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartUseCase cartUseCase;
    @Mock
    private CartApiMapper cartApiMapper;

    @InjectMocks
    private CartController controller;

    @Test
    void getActiveCart_returnsOk() {
        Cart cart = Cart.builder().id("c1").build();
        CartDtoOut dto = CartDtoOut.builder().id("c1").build();
        when(cartUseCase.getActiveCart("u1", "s1")).thenReturn(cart);
        when(cartApiMapper.toDto(cart)).thenReturn(dto);
        var resp = controller.getActiveCart("auth", "u1", "s1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void addItem_returnsCreated() {
        CartItemDtoIn in = CartItemDtoIn.builder().productId("p1").quantity(2).unitPrice(java.math.BigDecimal.TEN)
                .build();
        CartItem domain = CartItem.builder().productId("p1").build();
        Cart cart = Cart.builder().id("c1").build();
        CartDtoOut dto = CartDtoOut.builder().id("c1").build();
        when(cartApiMapper.toDomain(in)).thenReturn(domain);
        when(cartUseCase.addItem("u1", "s1", domain)).thenReturn(cart);
        when(cartApiMapper.toDto(cart)).thenReturn(dto);
        var resp = controller.addItem("auth", "u1", "s1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(201);
        assertThat(resp.getBody()).isSameAs(dto);
    }

    @Test
    void updateItemQuantity_returnsOk() {
        CartItemQuantityDtoIn in = CartItemQuantityDtoIn.builder().quantity(5).build();
        Cart cart = Cart.builder().id("c1").build();
        when(cartUseCase.updateItemQuantity("i1", 5)).thenReturn(cart);
        when(cartApiMapper.toDto(cart)).thenReturn(CartDtoOut.builder().build());
        var resp = controller.updateItemQuantity("auth", "i1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void removeItem_returnsOk() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartUseCase.removeItem("u1", "s1", "i1")).thenReturn(cart);
        when(cartApiMapper.toDto(cart)).thenReturn(CartDtoOut.builder().build());
        var resp = controller.removeItem("auth", "u1", "s1", "i1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void clearCart_returnsNoContent() {
        var resp = controller.clearCart("auth", "u1", "s1");
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(cartUseCase).clearCart("u1", "s1");
    }

    @Test
    void mergeCart_returnsOk() {
        Cart cart = Cart.builder().id("c1").build();
        CartMergeDtoIn in = CartMergeDtoIn.builder().sessionId("s1").build();
        when(cartUseCase.mergeCart("u1", "s1")).thenReturn(cart);
        when(cartApiMapper.toDto(cart)).thenReturn(CartDtoOut.builder().build());
        var resp = controller.mergeCart("auth", "u1", in);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getActiveCart_anonymous_passesNullUser() {
        Cart cart = Cart.builder().id("c1").build();
        when(cartUseCase.getActiveCart(null, "s1")).thenReturn(cart);
        when(cartApiMapper.toDto(any())).thenReturn(CartDtoOut.builder().build());
        var resp = controller.getActiveCart("auth", null, "s1");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
