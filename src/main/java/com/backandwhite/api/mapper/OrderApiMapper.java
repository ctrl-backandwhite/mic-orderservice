package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.out.OrderDtoOut;
import com.backandwhite.api.dto.out.OrderItemDtoOut;
import com.backandwhite.api.dto.out.OrderStatsDtoOut;
import com.backandwhite.api.dto.out.OrderStatusHistoryDtoOut;
import com.backandwhite.api.dto.out.RevenueByDayDtoOut;
import com.backandwhite.api.dto.out.StatusCountDtoOut;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.model.RevenueByDay;
import com.backandwhite.domain.model.StatusCount;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MoneyMapperHelper.class)
public interface OrderApiMapper {

    OrderDtoOut toDto(Order order);

    List<OrderDtoOut> toDtoList(List<Order> orders);

    OrderItemDtoOut toItemDto(OrderItem item);

    OrderStatusHistoryDtoOut toHistoryDto(OrderStatusHistory history);

    OrderStatsDtoOut toStatsDto(OrderStats stats);

    RevenueByDayDtoOut toRevenueByDayDto(RevenueByDay src);

    List<RevenueByDayDtoOut> toRevenueByDayDtoList(List<RevenueByDay> src);

    @Mapping(target = "status", expression = "java(src.getStatus() != null ? src.getStatus().name() : null)")
    StatusCountDtoOut toStatusCountDto(StatusCount src);

    List<StatusCountDtoOut> toStatusCountDtoList(List<StatusCount> src);
}
