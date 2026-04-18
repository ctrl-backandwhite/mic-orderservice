package com.backandwhite.domain.repository;

import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.OrderStatusHistory;
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
}
