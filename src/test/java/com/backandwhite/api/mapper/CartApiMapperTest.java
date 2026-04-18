package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.in.CartItemDtoIn;
import com.backandwhite.api.dto.out.CartDtoOut;
import com.backandwhite.api.dto.out.CartItemDtoOut;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.valueobject.CartStatus;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartApiMapperTest {

    private CartApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new CartApiMapperImpl();
        Field f = CartApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    @Test
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_mapsAllFields() {
        Cart cart = Cart.builder().id("c1").userId("u1").sessionId("s1").status(CartStatus.ACTIVE)
                .subtotal(Money.of(new BigDecimal("50.00"))).itemCount(2).items(List.of()).build();

        CartDtoOut dto = mapper.toDto(cart);
        assertThat(dto.getId()).isEqualTo("c1");
        assertThat(dto.getUserId()).isEqualTo("u1");
        assertThat(dto.getSubtotal()).isEqualByComparingTo("50.00");
    }

    @Test
    void toItemDto_null_returnsNull() {
        assertThat(mapper.toItemDto(null)).isNull();
    }

    @Test
    void toItemDto_mapsAllFields() {
        CartItem item = CartItem.builder().id("i1").productId("p1").variantId("v1").quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00"))).productName("Prod").productImage("img")
                .selectedAttrs(Map.of("color", "red")).build();

        CartItemDtoOut dto = mapper.toItemDto(item);
        assertThat(dto.getId()).isEqualTo("i1");
        assertThat(dto.getProductId()).isEqualTo("p1");
        assertThat(dto.getSelectedAttrs()).containsEntry("color", "red");
    }

    @Test
    void toItemDtoList_nullReturnsNull() {
        assertThat(mapper.toItemDtoList(null)).isNull();
    }

    @Test
    void toItemDtoList_mapsList() {
        CartItem item1 = CartItem.builder().id("i1").quantity(1).build();
        CartItem item2 = CartItem.builder().id("i2").quantity(2).build();

        List<CartItemDtoOut> list = mapper.toItemDtoList(List.of(item1, item2));
        assertThat(list).hasSize(2);
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        CartItemDtoIn dto = new CartItemDtoIn();
        dto.setProductId("p1");
        dto.setVariantId("v1");
        dto.setQuantity(3);
        dto.setUnitPrice(new BigDecimal("15.00"));
        dto.setProductName("Prod");
        dto.setSelectedAttrs(Map.of("size", "M"));

        CartItem item = mapper.toDomain(dto);
        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice().getAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void toDomain_nullQuantityNullAttrs_mapsPartial() {
        CartItemDtoIn dto = new CartItemDtoIn();
        dto.setProductId("p1");
        dto.setQuantity(null);
        dto.setUnitPrice(null);

        CartItem item = mapper.toDomain(dto);
        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getUnitPrice()).isNull();
    }

    @Test
    void toItemDto_nullAttrs_resultsInNull() {
        CartItem item = CartItem.builder().id("i1").selectedAttrs(null).build();
        CartItemDtoOut dto = mapper.toItemDto(item);
        assertThat(dto.getSelectedAttrs()).isNull();
    }
}
