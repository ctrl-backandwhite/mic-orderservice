package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.out.OrderDtoOut;
import com.backandwhite.api.dto.out.OrderItemDtoOut;
import com.backandwhite.api.dto.out.OrderStatsDtoOut;
import com.backandwhite.api.dto.out.OrderStatusHistoryDtoOut;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MoneyMapperHelper.class)
public interface OrderApiMapper {

    OrderDtoOut toDto(Order order);

    List<OrderDtoOut> toDtoList(List<Order> orders);

    OrderItemDtoOut toItemDto(OrderItem item);

    OrderStatusHistoryDtoOut toHistoryDto(OrderStatusHistory history);

    OrderStatsDtoOut toStatsDto(OrderStats stats);
}
