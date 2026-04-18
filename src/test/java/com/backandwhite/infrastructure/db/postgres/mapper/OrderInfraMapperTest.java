package com.backandwhite.infrastructure.db.postgres.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStatusHistoryEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderInfraMapperTest {

    private OrderInfraMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderInfraMapperImpl();
    }

    @Test
    void toDomain_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_full() {
        OrderEntity entity = OrderEntity.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("10"))).total(Money.of(new BigDecimal("12")))
                .shippingAddress(Map.of("country", "US")).billingAddress(Map.of("country", "US")).items(List.of())
                .statusHistory(List.of()).build();
        Order d = mapper.toDomain(entity);
        assertThat(d.getId()).isEqualTo("o1");
        assertThat(d.getShippingAddress()).containsEntry("country", "US");
    }

    @Test
    void toDomain_nullAddresses() {
        OrderEntity entity = OrderEntity.builder().id("o1").orderNumber("NX").userId("u1").status(OrderStatus.DRAFT)
                .shippingAddress(null).billingAddress(null).items(null).statusHistory(null).build();
        Order d = mapper.toDomain(entity);
        assertThat(d.getShippingAddress()).isNull();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_full() {
        Order d = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .shippingAddress(Map.of("c", "US")).billingAddress(Map.of("c", "US")).build();
        OrderEntity e = mapper.toEntity(d);
        assertThat(e.getId()).isEqualTo("o1");
        assertThat(e.getShippingAddress()).containsEntry("c", "US");
    }

    @Test
    void toEntity_nullAddresses() {
        Order d = Order.builder().id("o1").shippingAddress(null).billingAddress(null).build();
        OrderEntity e = mapper.toEntity(d);
        assertThat(e.getShippingAddress()).isNull();
    }

    @Test
    void toItemDomain_null() {
        assertThat(mapper.toItemDomain(null)).isNull();
    }

    @Test
    void toItemDomain_withOrder() {
        OrderEntity order = OrderEntity.builder().id("o1").build();
        OrderItemEntity entity = OrderItemEntity.builder().id("i1").order(order).productId("p1").quantity(1)
                .unitPrice(Money.of(new BigDecimal("10"))).totalPrice(Money.of(new BigDecimal("10"))).productName("P")
                .build();
        OrderItem d = mapper.toItemDomain(entity);
        assertThat(d.getOrderId()).isEqualTo("o1");
    }

    @Test
    void toItemDomain_nullOrder() {
        OrderItemEntity entity = OrderItemEntity.builder().id("i1").order(null).quantity(1).build();
        OrderItem d = mapper.toItemDomain(entity);
        assertThat(d.getOrderId()).isNull();
    }

    @Test
    void toItemEntity_null() {
        assertThat(mapper.toItemEntity(null)).isNull();
    }

    @Test
    void toItemEntity_full() {
        OrderItem d = OrderItem.builder().id("i1").orderId("o1").productId("p1").quantity(1).build();
        OrderItemEntity e = mapper.toItemEntity(d);
        assertThat(e.getId()).isEqualTo("i1");
    }

    @Test
    void toHistoryDomain_null() {
        assertThat(mapper.toHistoryDomain(null)).isNull();
    }

    @Test
    void toHistoryDomain_withOrder() {
        OrderEntity order = OrderEntity.builder().id("o1").build();
        OrderStatusHistoryEntity entity = OrderStatusHistoryEntity.builder().id("h1").order(order).fromStatus("DRAFT")
                .toStatus("PENDING").build();
        OrderStatusHistory d = mapper.toHistoryDomain(entity);
        assertThat(d.getOrderId()).isEqualTo("o1");
    }

    @Test
    void toHistoryDomain_nullOrder() {
        OrderStatusHistoryEntity entity = OrderStatusHistoryEntity.builder().id("h1").order(null).build();
        OrderStatusHistory d = mapper.toHistoryDomain(entity);
        assertThat(d.getOrderId()).isNull();
    }

    @Test
    void toHistoryEntity_null() {
        assertThat(mapper.toHistoryEntity(null)).isNull();
    }

    @Test
    void toHistoryEntity_full() {
        OrderStatusHistory d = OrderStatusHistory.builder().id("h1").orderId("o1").fromStatus("DRAFT")
                .toStatus("PENDING").build();
        OrderStatusHistoryEntity e = mapper.toHistoryEntity(d);
        assertThat(e.getId()).isEqualTo("h1");
    }

    @Test
    void toItemDomainList_null() {
        assertThat(mapper.toItemDomainList(null)).isNull();
    }

    @Test
    void toItemDomainList_mapsList() {
        assertThat(mapper.toItemDomainList(List.of(OrderItemEntity.builder().id("i1").quantity(1).build()))).hasSize(1);
    }

    @Test
    void toItemEntityList_null() {
        assertThat(mapper.toItemEntityList(null)).isNull();
    }

    @Test
    void toItemEntityList_mapsList() {
        assertThat(mapper.toItemEntityList(List.of(OrderItem.builder().id("i1").build()))).hasSize(1);
    }

    @Test
    void toHistoryDomainList_null() {
        assertThat(mapper.toHistoryDomainList(null)).isNull();
    }

    @Test
    void toHistoryDomainList_mapsList() {
        assertThat(mapper.toHistoryDomainList(List.of(OrderStatusHistoryEntity.builder().id("h1").build()))).hasSize(1);
    }
}
