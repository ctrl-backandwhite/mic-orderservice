package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
import com.backandwhite.domain.model.RevenueByDay;
import com.backandwhite.domain.model.StatusCount;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {
    Order save(Order order);

    Order update(Order order);

    Optional<Order> findById(String id);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findAll(Map<String, Object> filters, Pageable pageable);

    Page<Order> findByUserId(String userId, Map<String, Object> filters, Pageable pageable);

    void addStatusHistory(OrderStatusHistory history);

    OrderStats getStats();

    List<RevenueByDay> findRevenueByDay(Instant from, Instant to);

    List<StatusCount> findStatusDistribution(Instant from, Instant to);
}
