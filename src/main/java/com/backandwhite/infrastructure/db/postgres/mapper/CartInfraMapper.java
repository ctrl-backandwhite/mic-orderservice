package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.infrastructure.db.postgres.entity.CartEntity;
import com.backandwhite.infrastructure.db.postgres.entity.CartItemEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartInfraMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    Cart toDomain(CartEntity entity);

    @Mapping(target = "items", ignore = true)
    CartEntity toEntity(Cart domain);

    @Mapping(target = "cartId", source = "cart.id")
    CartItem toItemDomain(CartItemEntity entity);

    @Mapping(target = "cart", ignore = true)
    CartItemEntity toItemEntity(CartItem domain);

    List<CartItem> toItemDomainList(List<CartItemEntity> entities);
}
