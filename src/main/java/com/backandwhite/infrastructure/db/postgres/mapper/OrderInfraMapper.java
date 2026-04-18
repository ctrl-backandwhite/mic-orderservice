package com.backandwhite.infrastructure.db.postgres.mapper;

import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStatusHistoryEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderInfraMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "statusHistory", source = "statusHistory")
    Order toDomain(OrderEntity entity);

    @Mapping(target = "items", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    OrderEntity toEntity(Order domain);

    @Mapping(target = "orderId", source = "order.id")
    OrderItem toItemDomain(OrderItemEntity entity);

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toItemEntity(OrderItem domain);

    @Mapping(target = "orderId", source = "order.id")
    OrderStatusHistory toHistoryDomain(OrderStatusHistoryEntity entity);

    @Mapping(target = "order", ignore = true)
    OrderStatusHistoryEntity toHistoryEntity(OrderStatusHistory domain);

    List<OrderItem> toItemDomainList(List<OrderItemEntity> entities);

    List<OrderItemEntity> toItemEntityList(List<OrderItem> items);

    List<OrderStatusHistory> toHistoryDomainList(List<OrderStatusHistoryEntity> entities);
}
