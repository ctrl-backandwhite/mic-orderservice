package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.CartItemDtoIn;
import com.backandwhite.api.dto.out.CartDtoOut;
import com.backandwhite.api.dto.out.CartItemDtoOut;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MoneyMapperHelper.class)
public interface CartApiMapper {

    CartDtoOut toDto(Cart cart);

    CartItemDtoOut toItemDto(CartItem item);

    List<CartItemDtoOut> toItemDtoList(List<CartItem> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cartId", ignore = true)
    CartItem toDomain(CartItemDtoIn dto);
}
