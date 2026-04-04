package com.backandwhite.application.usecase.impl;

import com.backandwhite.api.dto.PaginationDtoOut;
import com.backandwhite.api.util.PageableUtils;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.domain.model.*;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.valureobject.CartStatus;
import com.backandwhite.domain.valureobject.CouponType;
import com.backandwhite.domain.valureobject.InvoiceStatus;
import com.backandwhite.domain.valureobject.OrderStatus;
import com.backandwhite.infrastructure.message.kafka.producer.OrderEventProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderUseCaseImpl implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CouponUseCase couponUseCase;
    private final ShippingTaxUseCase shippingTaxUseCase;
    private final InvoiceUseCase invoiceUseCase;
    private final Optional<OrderEventProducerService> orderEventProducer;

    @Override
    @Transactional
    public Order createFromCart(String userId, String sessionId,
            Map<String, Object> shippingAddress,
            Map<String, Object> billingAddress,
            String paymentMethod, String couponCode, String notes) {

        if (shippingAddress == null || shippingAddress.isEmpty()) {
            throw MAX_ADDRESSES_REACHED.toBusinessException();
        }

        // 1. Get the active cart
        Cart cart = cartRepository.findActiveByUserId(userId)
                .or(() -> cartRepository.findActiveBySessionId(sessionId))
                .orElseThrow(() -> CART_NOT_FOUND.toBusinessException());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw CART_EMPTY.toBusinessException();
        }

        BigDecimal subtotal = cart.getSubtotal();

        // 2. Calculate shipping
        String country = shippingAddress.getOrDefault("country", "").toString();
        BigDecimal shippingCost = BigDecimal.ZERO;
        List<ShippingRule> options = shippingTaxUseCase.findShippingOptions(country, BigDecimal.ONE, subtotal);
        if (!options.isEmpty()) {
            shippingCost = options.getFirst().getRate();
        }

        // 3. Calculate tax
        String region = shippingAddress.getOrDefault("region", "").toString();
        BigDecimal taxAmount = shippingTaxUseCase.calculateTax(country, region, subtotal);

        // 4. Apply coupon
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponId = null;
        boolean freeShipping = false;

        if (couponCode != null && !couponCode.isBlank()) {
            discountAmount = couponUseCase.validate(couponCode, subtotal, userId);

            Coupon coupon = couponUseCase.findByCode(couponCode);
            couponId = coupon.getId();
            freeShipping = coupon.getType() == CouponType.FREE_SHIPPING;
        }

        if (freeShipping) {
            shippingCost = BigDecimal.ZERO;
        }

        BigDecimal total = subtotal.add(shippingCost).add(taxAmount).subtract(discountAmount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 5. Build order items from cart items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(ci -> OrderItem.builder()
                        .productId(ci.getProductId())
                        .variantId(ci.getVariantId())
                        .sku(null)
                        .productName(ci.getProductName())
                        .productImage(ci.getProductImage())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .totalPrice(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .build())
                .toList();

        // 6. Build order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .total(total)
                .couponId(couponId)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress != null ? billingAddress : shippingAddress)
                .paymentMethod(paymentMethod)
                .notes(notes)
                .items(orderItems)
                .build();

        Order saved = orderRepository.save(order);

        // Publish order.created event
        orderEventProducer.ifPresent(p -> p.publishOrderCreated(
                saved.getId(), userId, null, saved.getOrderNumber(),
                saved.getTotal().toPlainString(), saved.getStatus().name(),
                saved.getItems().size(), null));

        // Deduct stock for each item via Kafka
        orderEventProducer.ifPresent(producer -> {
            for (OrderItem oi : saved.getItems()) {
                if (oi.getVariantId() != null && !oi.getVariantId().isBlank()) {
                    producer.publishStockDeducted(
                            oi.getProductId(),
                            oi.getVariantId(),
                            saved.getId(),
                            oi.getQuantity());
                }
            }
        });

        // 7. Auto-create invoice
        try {
            Map<String, Object> customerSnapshot = new LinkedHashMap<>();
            customerSnapshot.put("name", shippingAddress.getOrDefault("fullName", ""));
            customerSnapshot.put("phone", shippingAddress.getOrDefault("phone", ""));
            customerSnapshot.put("address", buildAddressString(shippingAddress));

            List<Map<String, Object>> invoiceLines = new ArrayList<>();
            for (OrderItem oi : saved.getItems()) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("name", oi.getProductName());
                line.put("sku", oi.getSku());
                line.put("quantity", oi.getQuantity());
                line.put("unitPrice", oi.getUnitPrice());
                line.put("total", oi.getTotalPrice());
                invoiceLines.add(line);
            }

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(generateInvoiceNumber())
                    .orderId(saved.getId())
                    .status(InvoiceStatus.PENDING)
                    .issueDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(30))
                    .subtotal(saved.getSubtotal())
                    .shipping(saved.getShippingCost())
                    .tax(saved.getTaxAmount())
                    .total(saved.getTotal())
                    .paymentMethod(saved.getPaymentMethod())
                    .customerSnapshot(customerSnapshot)
                    .lines(invoiceLines)
                    .notes(null)
                    .build();

            invoiceUseCase.create(invoice);
            log.info("Invoice {} created for order {}", invoice.getInvoiceNumber(), saved.getOrderNumber());
        } catch (Exception e) {
            log.warn("Failed to auto-create invoice for order {}: {}", saved.getId(), e.getMessage());
        }

        // 8. Apply coupon usage
        if (couponId != null) {
            couponUseCase.applyCouponToOrder(couponId, userId, saved.getId());
        }

        // 9. Mark cart as ordered
        cart.setStatus(CartStatus.ORDERED);
        cartRepository.update(cart);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<Order> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(orderRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationDtoOut<Order> findByUserId(String userId, Map<String, Object> filters, int page, int size,
            String sortBy, boolean ascending) {
        var pageable = PageableUtils.toPageable(page, size, sortBy, ascending);
        return PageableUtils.toResponse(orderRepository.findByUserId(userId, filters, pageable));
    }

    @Override
    @Transactional
    public Order updateStatus(String id, OrderStatus newStatus, String changedBy, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", id));

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw INVALID_STATUS_TRANSITION.toBusinessException(order.getStatus(), newStatus);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(order.getId())
                .fromStatus(order.getStatus().name())
                .toStatus(newStatus.name())
                .changedBy(changedBy)
                .reason(reason)
                .changedAt(Instant.now())
                .build();

        orderRepository.addStatusHistory(history);

        order.setStatus(newStatus);
        Order updated = orderRepository.update(order);

        // Publish order status event
        orderEventProducer.ifPresent(p -> p.publishOrderStatusUpdated(
                updated.getId(), updated.getUserId(), null,
                updated.getOrderNumber(),
                history.getFromStatus(), newStatus.name()));

        return updated;
    }

    @Override
    @Transactional
    public Order cancel(String id, String userId, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", id));

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw INVALID_STATUS_TRANSITION.toBusinessException(order.getStatus(), OrderStatus.CANCELLED);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(order.getId())
                .fromStatus(order.getStatus().name())
                .toStatus(OrderStatus.CANCELLED.name())
                .changedBy(userId)
                .reason(reason != null ? reason : "Cancelled by user")
                .changedAt(Instant.now())
                .build();

        orderRepository.addStatusHistory(history);

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.update(order);

        // Publish order.cancelled event
        orderEventProducer.ifPresent(p -> p.publishOrderCancelled(
                cancelled.getId(), userId, null,
                cancelled.getOrderNumber(), reason));

        return cancelled;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStats getStats() {
        return orderRepository.getStats();
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "NX-" + date + "-" + random;
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "FAC-NX-" + date + "-" + random;
    }

    private String buildAddressString(Map<String, Object> addr) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, addr, "street");
        appendIfPresent(sb, addr, "city");
        appendIfPresent(sb, addr, "region");
        appendIfPresent(sb, addr, "postalCode");
        appendIfPresent(sb, addr, "country");
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, Map<String, Object> addr, String key) {
        Object val = addr.get(key);
        if (val != null && !val.toString().isBlank()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(val);
        }
    }
}
