package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.valueobject.CartStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CartEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CartItemEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.CartInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.CartItemJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.CartJpaRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartRepositoryImplTest {

    @Mock
    private CartJpaRepository cartJpa;

    @Mock
    private CartItemJpaRepository cartItemJpa;

    @Mock
    private CartInfraMapper mapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CartRepositoryImpl adapter;

    private CartItem item(int qty, BigDecimal price) {
        return CartItem.builder().id("i").quantity(qty).unitPrice(Money.of(price)).build();
    }

    private Cart cartWithItems(List<CartItem> items) {
        return Cart.builder().id("c1").userId("u1").items(items).build();
    }

    @Test
    void findActiveByUserId_present_enrichesSubtotalAndCount() {
        CartEntity entity = new CartEntity();
        Cart domain = cartWithItems(List.of(item(2, new BigDecimal("10.00")), item(3, new BigDecimal("5.00"))));
        when(cartJpa.findByUserIdAndStatus("u1", CartStatus.ACTIVE)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Cart> result = adapter.findActiveByUserId("u1");
        assertThat(result).isPresent();
        assertThat(result.get().getItemCount()).isEqualTo(5);
        assertThat(result.get().getSubtotal().getAmount()).isEqualByComparingTo(new BigDecimal("35.00"));
    }

    @Test
    void findActiveByUserId_empty() {
        when(cartJpa.findByUserIdAndStatus("u1", CartStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThat(adapter.findActiveByUserId("u1")).isEmpty();
    }

    @Test
    void findActiveBySessionId_present_enrichesEvenWhenItemsNull() {
        CartEntity entity = new CartEntity();
        Cart domain = Cart.builder().id("c1").items(null).build();
        when(cartJpa.findBySessionIdAndStatus("s1", CartStatus.ACTIVE)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Cart> result = adapter.findActiveBySessionId("s1");
        assertThat(result).isPresent();
        assertThat(result.get().getItemCount()).isZero();
        assertThat(result.get().getSubtotal().isZero()).isTrue();
    }

    @Test
    void findActiveBySessionId_empty() {
        when(cartJpa.findBySessionIdAndStatus("s1", CartStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThat(adapter.findActiveBySessionId("s1")).isEmpty();
    }

    @Test
    void findById_present() {
        CartEntity entity = new CartEntity();
        Cart domain = cartWithItems(List.of());
        when(cartJpa.findById("c1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById("c1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(cartJpa.findById("c1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("c1")).isEmpty();
    }

    @Test
    void save_assignsUuidStatusAndExpiry() {
        Cart in = Cart.builder().userId("u1").build();
        CartEntity entity = new CartEntity();
        CartEntity saved = new CartEntity();
        Cart out = cartWithItems(List.of());

        when(mapper.toEntity(in)).thenReturn(entity);
        when(cartJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Cart result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(in.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(in.getExpiresAt()).isNotNull();
        assertThat(result).isNotNull();
    }

    @Test
    void update_delegatesEnrichment() {
        Cart in = Cart.builder().id("c1").userId("u1").build();
        CartEntity entity = new CartEntity();
        CartEntity saved = new CartEntity();
        Cart out = cartWithItems(List.of(item(1, new BigDecimal("4.00"))));

        when(mapper.toEntity(in)).thenReturn(entity);
        when(cartJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Cart result = adapter.update(in);
        assertThat(result.getItemCount()).isEqualTo(1);
        assertThat(result.getSubtotal().getAmount()).isEqualByComparingTo(new BigDecimal("4.00"));
    }

    @Test
    void delete_delegates() {
        adapter.delete("c1");
        verify(cartJpa).deleteById("c1");
    }

    @Test
    void addItem_assignsUuidAndAttachesCart() {
        CartItem item = CartItem.builder().productId("p1").build();
        CartItemEntity entity = new CartItemEntity();
        CartItemEntity saved = new CartItemEntity();
        CartEntity cartRef = new CartEntity();
        CartItem out = CartItem.builder().id("i1").build();

        when(mapper.toItemEntity(item)).thenReturn(entity);
        when(cartJpa.getReferenceById("c1")).thenReturn(cartRef);
        when(cartItemJpa.save(entity)).thenReturn(saved);
        when(mapper.toItemDomain(saved)).thenReturn(out);

        CartItem result = adapter.addItem("c1", item);
        assertThat(item.getId()).isNotBlank();
        assertThat(entity.getCart()).isSameAs(cartRef);
        assertThat(result).isSameAs(out);
    }

    @Test
    void updateItem_attachesCart() {
        CartItem item = CartItem.builder().id("i1").cartId("c1").build();
        CartItemEntity entity = new CartItemEntity();
        CartItemEntity saved = new CartItemEntity();
        CartEntity cartRef = new CartEntity();
        CartItem out = CartItem.builder().id("i1").build();

        when(mapper.toItemEntity(item)).thenReturn(entity);
        when(cartJpa.getReferenceById("c1")).thenReturn(cartRef);
        when(cartItemJpa.save(entity)).thenReturn(saved);
        when(mapper.toItemDomain(saved)).thenReturn(out);

        assertThat(adapter.updateItem(item)).isSameAs(out);
        assertThat(entity.getCart()).isSameAs(cartRef);
    }

    @Test
    void removeItem_deletesAndFlushes() {
        adapter.removeItem("i1");
        verify(cartItemJpa).deleteById("i1");
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    @Test
    void clearItems_delegates() {
        adapter.clearItems("c1");
        verify(cartItemJpa).deleteByCartId("c1");
    }

    @Test
    void findItemByCartAndProduct_present() {
        CartItemEntity e = new CartItemEntity();
        when(cartItemJpa.findByCartIdAndProductIdAndVariantId("c1", "p1", "v1")).thenReturn(Optional.of(e));
        when(mapper.toItemDomain(e)).thenReturn(CartItem.builder().id("i1").build());

        assertThat(adapter.findItemByCartAndProduct("c1", "p1", "v1")).isPresent();
    }

    @Test
    void findItemByCartAndProduct_empty() {
        when(cartItemJpa.findByCartIdAndProductIdAndVariantId("c1", "p1", "v1")).thenReturn(Optional.empty());
        assertThat(adapter.findItemByCartAndProduct("c1", "p1", "v1")).isEmpty();
    }

    @Test
    void findItemById_present() {
        CartItemEntity e = new CartItemEntity();
        when(cartItemJpa.findById("i1")).thenReturn(Optional.of(e));
        when(mapper.toItemDomain(e)).thenReturn(CartItem.builder().id("i1").build());

        assertThat(adapter.findItemById("i1")).isPresent();
    }

    @Test
    void findItemById_empty() {
        when(cartItemJpa.findById("i1")).thenReturn(Optional.empty());
        assertThat(adapter.findItemById("i1")).isEmpty();
    }

    @Test
    void enrichCart_throughSave_handlesEmptyItemsList() {
        // Triggering enrichCart via save when items list is empty
        Cart in = Cart.builder().userId("u1").build();
        CartEntity entity = new CartEntity();
        CartEntity saved = new CartEntity();
        Cart out = Cart.builder().id("c1").items(List.of()).build();

        when(mapper.toEntity(any(Cart.class))).thenReturn(entity);
        when(cartJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Cart result = adapter.save(in);
        assertThat(result.getItemCount()).isZero();
        assertThat(result.getSubtotal().isZero()).isTrue();
    }
}
