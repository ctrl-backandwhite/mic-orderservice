package com.backandwhite.application.usecase;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public interface OrderUseCase {
        Order createFromCart(String userId, String sessionId, Map<String, Object> shippingAddress,
                        Map<String, Object> billingAddress, String paymentMethod, String couponCode,
                        String giftCardCode, BigDecimal giftCardAmount,
                        Integer loyaltyPointsUsed, BigDecimal loyaltyDiscount,
                        String notes, String currencyCode);

        Order confirmOrder(String orderId, String userId, String email);

        Order findById(String id);

        Order findByOrderNumber(String orderNumber);

        PageResult<Order> findAll(Map<String, Object> filters, int page, int size, String sortBy,
                        boolean ascending);

        PageResult<Order> findByUserId(String userId, Map<String, Object> filters, int page, int size,
                        String sortBy,
                        boolean ascending);

        Order updateStatus(String id, OrderStatus newStatus, String changedBy, String reason);

        Order cancel(String id, String userId, String reason);

        OrderStats getStats();
}
