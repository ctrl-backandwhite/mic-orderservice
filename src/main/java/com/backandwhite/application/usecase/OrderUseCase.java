package com.backandwhite.application.usecase;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.valureobject.OrderStatus;

import java.util.Map;

public interface OrderUseCase {
        Order createFromCart(String userId, String sessionId, Map<String, Object> shippingAddress,
                        Map<String, Object> billingAddress, String paymentMethod, String couponCode, String notes);

        Order confirmOrder(String orderId, String userId);

        Order findById(String id);

        Order findByOrderNumber(String orderNumber);

        PaginationDtoOut<Order> findAll(Map<String, Object> filters, int page, int size, String sortBy,
                        boolean ascending);

        PaginationDtoOut<Order> findByUserId(String userId, Map<String, Object> filters, int page, int size,
                        String sortBy,
                        boolean ascending);

        Order updateStatus(String id, OrderStatus newStatus, String changedBy, String reason);

        Order cancel(String id, String userId, String reason);

        OrderStats getStats();
}
