package com.backandwhite.infrastructure.db.postgres.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.model.RevenueByDay;
import com.backandwhite.domain.model.StatusCount;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStatusHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.OrderInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.OrderJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderStatusHistoryJpaRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryImplTest {

    @Mock
    private OrderJpaRepository orderJpa;

    @Mock
    private OrderStatusHistoryJpaRepository historyJpa;

    @Mock
    private OrderInfraMapper mapper;

    @InjectMocks
    private OrderRepositoryImpl adapter;

    private Order order(String id) {
        return Order.builder().id(id).orderNumber("ORD-1").userId("u1").subtotal(Money.of(new BigDecimal("10.00")))
                .shippingCost(Money.of(new BigDecimal("2.00"))).taxAmount(Money.of(new BigDecimal("1.00")))
                .discountAmount(Money.of(BigDecimal.ZERO)).total(Money.of(new BigDecimal("13.00"))).build();
    }

    @Test
    void save_withoutItems_assignsUuidAndDelegates() {
        Order in = Order.builder().userId("u1").items(null).build();
        OrderEntity entity = new OrderEntity();
        OrderEntity saved = new OrderEntity();
        Order out = order("x");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(orderJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Order result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(result).isSameAs(out);
        assertThat(entity.getItems()).isEmpty();
    }

    @Test
    void save_withItems_assignsItemUuidsAndAttachesOrder() {
        OrderItem item = OrderItem.builder().productId("p1").quantity(1).build();
        Order in = Order.builder().userId("u1").items(List.of(item)).build();
        OrderEntity entity = new OrderEntity();
        OrderEntity saved = new OrderEntity();
        OrderItemEntity itemEntity = new OrderItemEntity();
        Order out = order("x");

        when(mapper.toEntity(in)).thenReturn(entity);
        when(mapper.toItemEntity(item)).thenReturn(itemEntity);
        when(orderJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Order result = adapter.save(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(itemEntity.getId()).isNotBlank();
        assertThat(itemEntity.getOrder()).isSameAs(entity);
        assertThat(entity.getItems()).hasSize(1);
        assertThat(result).isSameAs(out);
    }

    @Test
    void update_existing_mergesScalarFields() {
        Order in = order("ord-1");
        in.setStatus(OrderStatus.CONFIRMED);
        in.setNotes("note");

        OrderEntity existing = new OrderEntity();
        existing.setStatus(OrderStatus.PENDING);
        OrderEntity saved = new OrderEntity();
        Order out = order("ord-1");

        when(orderJpa.findById("ord-1")).thenReturn(Optional.of(existing));
        when(orderJpa.save(existing)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(out);

        Order result = adapter.update(in);
        assertThat(result).isSameAs(out);
        assertThat(existing.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(existing.getNotes()).isEqualTo("note");
        assertThat(existing.getSubtotal().getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void update_nullStatus_preservesExistingStatus() {
        Order in = order("ord-1");
        in.setStatus(null);

        OrderEntity existing = new OrderEntity();
        existing.setStatus(OrderStatus.PROCESSING);
        OrderEntity saved = new OrderEntity();

        when(orderJpa.findById("ord-1")).thenReturn(Optional.of(existing));
        when(orderJpa.save(existing)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(order("ord-1"));

        adapter.update(in);
        assertThat(existing.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void update_notFound_throws() {
        Order in = order("missing");
        when(orderJpa.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.update(in)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findById_present() {
        OrderEntity e = new OrderEntity();
        when(orderJpa.findById("ord-1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(order("ord-1"));
        assertThat(adapter.findById("ord-1")).isPresent();
    }

    @Test
    void findById_empty() {
        when(orderJpa.findById("ord-1")).thenReturn(Optional.empty());
        assertThat(adapter.findById("ord-1")).isEmpty();
    }

    @Test
    void findByOrderNumber_present() {
        OrderEntity e = new OrderEntity();
        when(orderJpa.findByOrderNumber("ORD-1")).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(order("ord-1"));
        assertThat(adapter.findByOrderNumber("ORD-1")).isPresent();
    }

    @Test
    void findByOrderNumber_empty() {
        when(orderJpa.findByOrderNumber("ORD-1")).thenReturn(Optional.empty());
        assertThat(adapter.findByOrderNumber("ORD-1")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_delegates() {
        OrderEntity e = new OrderEntity();
        Page<OrderEntity> p = new PageImpl<>(List.of(e));
        when(orderJpa.findAll(any(Specification.class), eq(PageRequest.of(0, 5)))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(order("ord-1"));

        assertThat(adapter.findAll(new HashMap<>(), PageRequest.of(0, 5)).getContent()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUserId_addsUserIdFilter() {
        OrderEntity e = new OrderEntity();
        Page<OrderEntity> p = new PageImpl<>(List.of(e));
        when(orderJpa.findAll(any(Specification.class), eq(PageRequest.of(0, 5)))).thenReturn(p);
        when(mapper.toDomain(e)).thenReturn(order("ord-1"));

        Map<String, Object> filters = new HashMap<>();
        Page<Order> result = adapter.findByUserId("u-x", filters, PageRequest.of(0, 5));
        assertThat(result.getContent()).hasSize(1);
        assertThat(filters).containsEntry("userId", "u-x");
    }

    @Test
    void addStatusHistory_assignsUuidAndAttachesOrder() {
        OrderStatusHistory in = OrderStatusHistory.builder().orderId("ord-1").toStatus("CONFIRMED").build();
        OrderStatusHistoryEntity entity = new OrderStatusHistoryEntity();
        OrderEntity orderRef = new OrderEntity();

        when(mapper.toHistoryEntity(in)).thenReturn(entity);
        when(orderJpa.getReferenceById("ord-1")).thenReturn(orderRef);

        adapter.addStatusHistory(in);
        assertThat(in.getId()).isNotBlank();
        assertThat(entity.getOrder()).isSameAs(orderRef);
        verify(historyJpa).save(entity);
    }

    @Test
    void getStats_calculatesAverage() {
        when(orderJpa.countAll()).thenReturn(10L);
        when(orderJpa.countPending()).thenReturn(2L);
        when(orderJpa.countProcessing()).thenReturn(1L);
        when(orderJpa.countShipped()).thenReturn(2L);
        when(orderJpa.countDelivered()).thenReturn(3L);
        when(orderJpa.countCancelled()).thenReturn(2L);
        when(orderJpa.sumTotalRevenue()).thenReturn(new BigDecimal("80.00"));

        OrderStats stats = adapter.getStats();
        assertThat(stats.getTotalOrders()).isEqualTo(10);
        assertThat(stats.getCancelledOrders()).isEqualTo(2);
        assertThat(stats.getTotalRevenue().getAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        // 80 / (10 - 2) = 10
        assertThat(stats.getAvgOrderValue().getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void getStats_allCancelled_avgZero() {
        when(orderJpa.countAll()).thenReturn(2L);
        when(orderJpa.countPending()).thenReturn(0L);
        when(orderJpa.countProcessing()).thenReturn(0L);
        when(orderJpa.countShipped()).thenReturn(0L);
        when(orderJpa.countDelivered()).thenReturn(0L);
        when(orderJpa.countCancelled()).thenReturn(2L);
        when(orderJpa.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);

        OrderStats stats = adapter.getStats();
        assertThat(stats.getAvgOrderValue().isZero()).isTrue();
    }

    @Test
    void findRevenueByDay_handlesSqlDate() {
        Date day = Date.valueOf(LocalDate.of(2025, 1, 1));
        Object[] row = new Object[]{day, new BigDecimal("100.00"), 5, new BigDecimal("10.00"), new BigDecimal("5.00")};
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-02-01T00:00:00Z");
        when(orderJpa.findRevenueByDay(from, to)).thenReturn(java.util.Collections.singletonList(row));

        List<RevenueByDay> result = adapter.findRevenueByDay(from, to);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDay()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.get(0).getOrders()).isEqualTo(5L);
        assertThat(result.get(0).getRefunded()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.get(0).getCancelled()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void findRevenueByDay_handlesLocalDate() {
        LocalDate day = LocalDate.of(2025, 1, 2);
        // toBigDecimal also covers the null branch and the Number branch via different
        // rows
        Object[] row1 = new Object[]{day, new BigDecimal("50.00"), 2L, null, 1.5};
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-02-01T00:00:00Z");
        when(orderJpa.findRevenueByDay(from, to)).thenReturn(java.util.Collections.singletonList(row1));

        List<RevenueByDay> result = adapter.findRevenueByDay(from, to);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDay()).isEqualTo(day);
        assertThat(result.get(0).getRefunded()).isEqualByComparingTo(BigDecimal.ZERO);
        // double 1.5 → BigDecimal.valueOf(1.5)
        assertThat(result.get(0).getCancelled()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    void findStatusDistribution_delegates() {
        Object[] row = new Object[]{OrderStatus.SHIPPED, 4L};
        Instant from = Instant.parse("2025-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-02-01T00:00:00Z");
        when(orderJpa.findStatusDistribution(from, to)).thenReturn(java.util.Collections.singletonList(row));

        List<StatusCount> result = adapter.findStatusDistribution(from, to);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result.get(0).getCount()).isEqualTo(4L);
    }

    @Test
    void save_withNullItems_doesNotInteractWithItemEntities() {
        Order in = Order.builder().userId("u1").items(null).build();
        OrderEntity entity = new OrderEntity();
        OrderEntity saved = new OrderEntity();

        when(mapper.toEntity(in)).thenReturn(entity);
        when(orderJpa.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(order("x"));

        adapter.save(in);
        verify(mapper, never()).toItemEntity(any());
        verify(orderJpa, times(1)).save(any(OrderEntity.class));
    }
}
