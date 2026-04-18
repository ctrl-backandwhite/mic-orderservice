package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.valueobject.CartStatus;
import com.backandwhite.infrastructure.db.postgres.entity.CartEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CartItemEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartInfraMapperTest {

    private CartInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new CartInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        CartEntity entity = CartEntity.builder().id("c1").userId("u1").sessionId("s1").status(CartStatus.ACTIVE)
                .items(List.of()).build();
        Cart domain = mapper.toDomain(entity);
        assertThat(domain.getId()).isEqualTo("c1");
        assertThat(domain.getUserId()).isEqualTo("u1");
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        Cart domain = Cart.builder().id("c1").userId("u1").sessionId("s1").status(CartStatus.ACTIVE).build();
        CartEntity e = mapper.toEntity(domain);
        assertThat(e.getId()).isEqualTo("c1");
    }

    @Test
    void toItemDomain_null() {
        assertThat(mapper.toItemDomain(null)).isNull();
    }

    @Test
    void toItemDomain_withCart() {
        CartEntity cart = CartEntity.builder().id("c1").build();
        CartItemEntity entity = CartItemEntity.builder().id("i1").cart(cart).productId("p1").variantId("v1").quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00"))).productName("Prod").selectedAttrs(Map.of("color", "red"))
                .build();

        CartItem d = mapper.toItemDomain(entity);
        assertThat(d.getId()).isEqualTo("i1");
        assertThat(d.getCartId()).isEqualTo("c1");
        assertThat(d.getSelectedAttrs()).containsEntry("color", "red");
    }

    @Test
    void toItemDomain_nullCart() {
        CartItemEntity entity = CartItemEntity.builder().id("i1").cart(null).quantity(1).build();
        CartItem d = mapper.toItemDomain(entity);
        assertThat(d.getCartId()).isNull();
    }

    @Test
    void toItemEntity_null() {
        assertThat(mapper.toItemEntity(null)).isNull();
    }

    @Test
    void toItemEntity_full() {
        CartItem item = CartItem.builder().id("i1").cartId("c1").productId("p1").variantId("v1").quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00"))).productName("Prod").selectedAttrs(Map.of("x", "y"))
                .build();

        CartItemEntity entity = mapper.toItemEntity(item);
        assertThat(entity.getId()).isEqualTo("i1");
        assertThat(entity.getCartId()).isEqualTo("c1");
        assertThat(entity.getSelectedAttrs()).containsEntry("x", "y");
    }

    @Test
    void toItemEntity_nullAttrs() {
        CartItem item = CartItem.builder().id("i1").selectedAttrs(null).build();
        CartItemEntity entity = mapper.toItemEntity(item);
        assertThat(entity.getSelectedAttrs()).isNull();
    }

    @Test
    void toItemDomainList_null() {
        assertThat(mapper.toItemDomainList(null)).isNull();
    }

    @Test
    void toItemDomainList_mapsList() {
        CartItemEntity e = CartItemEntity.builder().id("i1").quantity(1).build();
        assertThat(mapper.toItemDomainList(List.of(e))).hasSize(1);
    }
}
