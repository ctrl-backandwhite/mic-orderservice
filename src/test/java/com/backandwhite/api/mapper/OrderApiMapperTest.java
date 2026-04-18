package com.backandwhite.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.out.OrderDtoOut;
import com.backandwhite.api.dto.out.OrderStatsDtoOut;
import com.backandwhite.api.dto.out.OrderStatusHistoryDtoOut;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderApiMapperTest {

    private OrderApiMapperImpl mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new OrderApiMapperImpl();
        Field f = OrderApiMapperImpl.class.getDeclaredField("moneyMapperHelper");
        f.setAccessible(true);
        f.set(mapper, new MoneyMapperHelper());
    }

    @Test
    void toDto_null_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDto_full() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").status(OrderStatus.PENDING)
                .subtotal(Money.of(new BigDecimal("100"))).shippingCost(Money.of(new BigDecimal("5")))
                .taxAmount(Money.of(new BigDecimal("10"))).total(Money.of(new BigDecimal("115")))
                .shippingAddress(Map.of("country", "US")).billingAddress(Map.of("country", "US"))
                .items(List.of(OrderItem.builder().id("i1").build()))
                .statusHistory(
                        List.of(OrderStatusHistory.builder().id("h1").fromStatus("DRAFT").toStatus("PENDING").build()))
                .build();

        OrderDtoOut dto = mapper.toDto(order);
        assertThat(dto.getId()).isEqualTo("o1");
        assertThat(dto.getSubtotal()).isEqualByComparingTo("100");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getStatusHistory()).hasSize(1);
    }

    @Test
    void toDto_nullAddresses_notMapped() {
        Order order = Order.builder().id("o1").shippingAddress(null).billingAddress(null).items(null)
                .statusHistory(null).build();
        OrderDtoOut dto = mapper.toDto(order);
        assertThat(dto.getShippingAddress()).isNull();
        assertThat(dto.getItems()).isNull();
    }

    @Test
    void toDtoList_null_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toDtoList_mapsList() {
        List<OrderDtoOut> list = mapper.toDtoList(List.of(Order.builder().id("a").build()));
        assertThat(list).hasSize(1);
    }

    @Test
    void toItemDto_null() {
        assertThat(mapper.toItemDto(null)).isNull();
    }

    @Test
    void toItemDto_mapsAll() {
        OrderItem item = OrderItem.builder().id("i1").productId("p1").sku("S1").productName("P").quantity(2)
                .unitPrice(Money.of(new BigDecimal("10"))).totalPrice(Money.of(new BigDecimal("20")))
                .campaignId("camp1").campaignDiscount(Money.of(new BigDecimal("1"))).build();
        var dto = mapper.toItemDto(item);
        assertThat(dto.getId()).isEqualTo("i1");
        assertThat(dto.getUnitPrice()).isEqualByComparingTo("10");
    }

    @Test
    void toHistoryDto_null() {
        assertThat(mapper.toHistoryDto(null)).isNull();
    }

    @Test
    void toHistoryDto_nullStatuses() {
        OrderStatusHistory h = OrderStatusHistory.builder().id("h1").build();
        OrderStatusHistoryDtoOut dto = mapper.toHistoryDto(h);
        assertThat(dto.getId()).isEqualTo("h1");
        assertThat(dto.getFromStatus()).isNull();
        assertThat(dto.getToStatus()).isNull();
    }

    @Test
    void toHistoryDto_mapsFields() {
        OrderStatusHistory h = OrderStatusHistory.builder().id("h1").fromStatus("DRAFT").toStatus("PENDING")
                .changedBy("admin").reason("r").build();
        OrderStatusHistoryDtoOut dto = mapper.toHistoryDto(h);
        assertThat(dto.getFromStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(dto.getToStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void toStatsDto_null() {
        assertThat(mapper.toStatsDto(null)).isNull();
    }

    @Test
    void toStatsDto_mapsAll() {
        OrderStats stats = OrderStats.builder().totalOrders(10).pendingOrders(1).processingOrders(2).shippedOrders(3)
                .deliveredOrders(4).cancelledOrders(0).totalRevenue(Money.of(new BigDecimal("1000")))
                .avgOrderValue(Money.of(new BigDecimal("100"))).build();
        OrderStatsDtoOut dto = mapper.toStatsDto(stats);
        assertThat(dto.getTotalOrders()).isEqualTo(10);
        assertThat(dto.getTotalRevenue()).isEqualByComparingTo("1000");
    }
}
