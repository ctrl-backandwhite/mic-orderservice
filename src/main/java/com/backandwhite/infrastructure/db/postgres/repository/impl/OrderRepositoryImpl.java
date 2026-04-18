package com.backandwhite.infrastructure.db.postgres.repository.impl;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderItemEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderStatusHistoryEntity;
import com.backandwhite.infrastructure.db.postgres.mapper.OrderInfraMapper;
import com.backandwhite.infrastructure.db.postgres.repository.OrderJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderStatusHistoryJpaRepository;
import com.backandwhite.infrastructure.db.postgres.specification.OrderSpecification;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpa;
    private final OrderStatusHistoryJpaRepository historyJpa;
    private final OrderInfraMapper mapper;

    @Override
    public Order save(Order order) {
        order.setId(UUID.randomUUID().toString());
        OrderEntity entity = mapper.toEntity(order);

        if (order.getItems() != null) {
            List<OrderItemEntity> itemEntities = order.getItems().stream().map(item -> {
                OrderItemEntity ie = mapper.toItemEntity(item);
                ie.setId(UUID.randomUUID().toString());
                ie.setOrder(entity);
                return ie;
            }).toList();
            entity.getItems().addAll(itemEntities);
        }

        return mapper.toDomain(orderJpa.save(entity));
    }

    @Override
    public Order update(Order order) {
        OrderEntity existing = orderJpa.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + order.getId()));

        // Update only scalar fields, preserve items & statusHistory
        existing.setStatus(order.getStatus() != null ? order.getStatus() : existing.getStatus());
        existing.setSubtotal(order.getSubtotal());
        existing.setShippingCost(order.getShippingCost());
        existing.setTaxAmount(order.getTaxAmount());
        existing.setDiscountAmount(order.getDiscountAmount());
        existing.setTotal(order.getTotal());
        existing.setCouponId(order.getCouponId());
        existing.setGiftCardCode(order.getGiftCardCode());
        existing.setGiftCardAmount(order.getGiftCardAmount());
        existing.setLoyaltyPointsUsed(order.getLoyaltyPointsUsed());
        existing.setLoyaltyDiscount(order.getLoyaltyDiscount());
        existing.setShippingAddress(order.getShippingAddress());
        existing.setBillingAddress(order.getBillingAddress());
        existing.setPaymentMethod(order.getPaymentMethod());
        existing.setPaymentRef(order.getPaymentRef());
        existing.setNotes(order.getNotes());

        return mapper.toDomain(orderJpa.save(existing));
    }

    @Override
    public Optional<Order> findById(String id) {
        return orderJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderJpa.findByOrderNumber(orderNumber).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findAll(Map<String, Object> filters, Pageable pageable) {
        return orderJpa.findAll(OrderSpecification.withFilters(filters), pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findByUserId(String userId, Map<String, Object> filters, Pageable pageable) {
        filters.put("userId", userId);
        return orderJpa.findAll(OrderSpecification.withFilters(filters), pageable).map(mapper::toDomain);
    }

    @Override
    public void addStatusHistory(OrderStatusHistory history) {
        history.setId(UUID.randomUUID().toString());
        OrderStatusHistoryEntity entity = mapper.toHistoryEntity(history);
        entity.setOrder(orderJpa.getReferenceById(history.getOrderId()));
        historyJpa.save(entity);
    }

    @Override
    public OrderStats getStats() {
        long total = orderJpa.countAll();
        long pending = orderJpa.countPending();
        long processing = orderJpa.countProcessing();
        long shipped = orderJpa.countShipped();
        long delivered = orderJpa.countDelivered();
        long cancelled = orderJpa.countCancelled();
        BigDecimal revenue = orderJpa.sumTotalRevenue();
        Money totalRevenue = Money.of(revenue);
        Money avg = (total - cancelled) > 0 ? totalRevenue.divide(BigDecimal.valueOf(total - cancelled)) : Money.zero();

        return OrderStats.builder().totalOrders(total).pendingOrders(pending).processingOrders(processing)
                .shippedOrders(shipped).deliveredOrders(delivered).cancelledOrders(cancelled).totalRevenue(totalRevenue)
                .avgOrderValue(avg).build();
    }
}
