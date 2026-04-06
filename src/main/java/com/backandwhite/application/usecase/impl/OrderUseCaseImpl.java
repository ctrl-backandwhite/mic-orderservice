package com.backandwhite.application.usecase.impl;

import com.backandwhite.common.domain.model.PageResult;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.domain.model.*;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.valueobject.CartStatus;
import com.backandwhite.domain.valueobject.CouponType;
import com.backandwhite.domain.valueobject.InvoiceStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import com.backandwhite.application.port.out.CatalogPort;
import com.backandwhite.application.port.out.CatalogPort.ProductVerification;
import com.backandwhite.application.port.out.CmsPort;
import com.backandwhite.application.port.out.OrderEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final CatalogPort catalogClient;
    private final CmsPort cmsClient;
    private final OrderEventPort orderEventPort;

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

        // 1.5 Validate prices and stock against the product catalog (C-01, C-04, C-02)
        List<Map<String, Object>> activeCampaigns = cmsClient.getActiveCampaigns();
        Map<String, String> productCategoryMap = new HashMap<>(); // productId → categoryId
        BigDecimal totalWeight = BigDecimal.ZERO; // sum of item weights for shipping (M-03)

        for (CartItem ci : cart.getItems()) {
            // Verify unit price against catalog's actual price + active campaigns
            Optional<ProductVerification> verification = catalogClient.getVerifiedPriceAndCategory(
                    ci.getProductId(), ci.getVariantId());
            if (verification.isPresent()) {
                BigDecimal basePrice = verification.get().price();
                String categoryId = verification.get().categoryId();
                BigDecimal itemWeight = verification.get().weight();
                productCategoryMap.put(ci.getProductId(), categoryId);

                // Accumulate weight (M-03)
                if (itemWeight != null && itemWeight.compareTo(BigDecimal.ZERO) > 0) {
                    totalWeight = totalWeight.add(
                            itemWeight.multiply(BigDecimal.valueOf(ci.getQuantity())));
                }

                // Apply best campaign discount (server-side — C-02)
                BigDecimal campaignDiscount = cmsClient.calculateBestCampaignDiscount(
                        activeCampaigns, ci.getProductId(), categoryId, basePrice);
                BigDecimal verifiedPrice = basePrice.subtract(campaignDiscount);
                if (verifiedPrice.compareTo(BigDecimal.ZERO) < 0) {
                    verifiedPrice = BigDecimal.ZERO;
                }

                if (ci.getUnitPrice().compareTo(verifiedPrice) != 0) {
                    log.warn(
                            "Price correction: product={}, variant={}, cart={}, verified={} (base={}, campaign discount={})",
                            ci.getProductId(), ci.getVariantId(),
                            ci.getUnitPrice(), verifiedPrice, basePrice, campaignDiscount);
                    ci.setUnitPrice(verifiedPrice);
                }
            }

            // Check stock availability before creating the order (C-04)
            if (ci.getVariantId() != null && !ci.getVariantId().isBlank()) {
                int available = catalogClient.getAvailableStock(ci.getVariantId());
                if (available >= 0 && available < ci.getQuantity()) {
                    throw INSUFFICIENT_STOCK.toBusinessException(
                            ci.getProductName(), ci.getQuantity(), available);
                }
            }
        }

        // Fallback weight if no variant weights found
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalWeight = BigDecimal.ONE;
        }

        // Recalculate subtotal from verified prices
        BigDecimal subtotal = cart.getItems().stream()
                .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calculate shipping (using real product weight — M-03)
        String country = shippingAddress.getOrDefault("country", "").toString();
        BigDecimal shippingCost = BigDecimal.ZERO;
        List<ShippingRule> options = shippingTaxUseCase.findShippingOptions(country, totalWeight, subtotal);
        if (!options.isEmpty()) {
            shippingCost = options.getFirst().getRate();
        }

        // 3. Calculate tax
        String region = shippingAddress.getOrDefault("region", "").toString();
        BigDecimal taxAmount = shippingTaxUseCase.calculateTax(country, region, subtotal);

        // 4. Apply coupon (with scope filtering for appliesToProducts/Categories —
        // M-05)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponId = null;
        boolean freeShipping = false;

        if (couponCode != null && !couponCode.isBlank()) {
            // Basic validation (active, not expired, usage limits, min order)
            couponUseCase.validate(couponCode, subtotal, userId);

            Coupon coupon = couponUseCase.findByCode(couponCode);
            couponId = coupon.getId();
            freeShipping = coupon.getType() == CouponType.FREE_SHIPPING;

            // Check coupon scope and calculate eligible subtotal (M-05)
            BigDecimal eligibleSubtotal = calculateEligibleSubtotal(
                    coupon, cart.getItems(), productCategoryMap);

            boolean hasScopeRestrictions = (coupon.getAppliesToProducts() != null
                    && !coupon.getAppliesToProducts().isEmpty())
                    || (coupon.getAppliesToCategories() != null && !coupon.getAppliesToCategories().isEmpty());

            if (hasScopeRestrictions && eligibleSubtotal.compareTo(BigDecimal.ZERO) == 0) {
                throw COUPON_SCOPE_MISMATCH.toBusinessException();
            }

            // Calculate discount on eligible subtotal only
            BigDecimal discountBase = hasScopeRestrictions ? eligibleSubtotal : subtotal;
            discountAmount = calculateCouponDiscount(coupon, discountBase);
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
                .status(OrderStatus.DRAFT)
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

        // Mark cart as ordered (prevent reuse)
        cart.setStatus(CartStatus.ORDERED);
        cartRepository.update(cart);

        return saved;
    }

    @Override
    @Transactional
    public Order confirmOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ENTITY_NOT_FOUND.toEntityNotFound("Order", orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw INVALID_STATUS_TRANSITION.toBusinessException(order.getStatus(), OrderStatus.PENDING);
        }

        // Transition to PENDING
        order.setStatus(OrderStatus.PENDING);
        Order confirmed = orderRepository.update(order);

        // Record status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(confirmed.getId())
                .fromStatus(OrderStatus.DRAFT.name())
                .toStatus(OrderStatus.PENDING.name())
                .changedBy(userId)
                .reason("Payment confirmed — order activated")
                .changedAt(Instant.now())
                .build();
        orderRepository.addStatusHistory(history);

        // Publish order.created event
        orderEventPort.publishOrderCreated(
                confirmed.getId(), userId, null, confirmed.getOrderNumber(),
                confirmed.getTotal().toPlainString(), confirmed.getStatus().name(),
                confirmed.getItems().size(), null);

        // Deduct stock for each item via Kafka
        for (OrderItem oi : confirmed.getItems()) {
            if (oi.getVariantId() != null && !oi.getVariantId().isBlank()) {
                orderEventPort.publishStockDeducted(
                        oi.getProductId(),
                        oi.getVariantId(),
                        confirmed.getId(),
                        oi.getQuantity());
            }
        }

        // Auto-create invoice
        try {
            Map<String, Object> customerSnapshot = new LinkedHashMap<>();
            customerSnapshot.put("name", confirmed.getShippingAddress().getOrDefault("fullName", ""));
            customerSnapshot.put("phone", confirmed.getShippingAddress().getOrDefault("phone", ""));
            customerSnapshot.put("address", buildAddressString(confirmed.getShippingAddress()));

            List<Map<String, Object>> invoiceLines = new ArrayList<>();
            for (OrderItem oi : confirmed.getItems()) {
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
                    .orderId(confirmed.getId())
                    .status(InvoiceStatus.PENDING)
                    .issueDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(30))
                    .subtotal(confirmed.getSubtotal())
                    .shipping(confirmed.getShippingCost())
                    .tax(confirmed.getTaxAmount())
                    .total(confirmed.getTotal())
                    .paymentMethod(confirmed.getPaymentMethod())
                    .customerSnapshot(customerSnapshot)
                    .lines(invoiceLines)
                    .notes(null)
                    .build();

            invoiceUseCase.create(invoice);
            log.info("Invoice {} created for order {}", invoice.getInvoiceNumber(), confirmed.getOrderNumber());
        } catch (Exception e) {
            log.warn("Failed to auto-create invoice for order {}: {}", confirmed.getId(), e.getMessage());
        }

        // Apply coupon usage
        if (confirmed.getCouponId() != null) {
            couponUseCase.applyCouponToOrder(confirmed.getCouponId(), userId, confirmed.getId());
        }

        return confirmed;
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
    public PageResult<Order> findAll(Map<String, Object> filters, int page, int size, String sortBy,
            boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(orderRepository.findAll(filters, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> findByUserId(String userId, Map<String, Object> filters, int page, int size,
            String sortBy, boolean ascending) {
        var pageable = PageRequest.of(page, size,
                ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        return PageResult.from(orderRepository.findByUserId(userId, filters, pageable));
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
        orderEventPort.publishOrderStatusUpdated(
                updated.getId(), updated.getUserId(), null,
                updated.getOrderNumber(),
                history.getFromStatus(), newStatus.name());

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
        orderEventPort.publishOrderCancelled(
                cancelled.getId(), userId, null,
                cancelled.getOrderNumber(), reason);

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

    /**
     * Calculates the subtotal of cart items that match the coupon's scope
     * restrictions.
     * If the coupon has no scope (appliesToProducts/Categories both empty), returns
     * full subtotal.
     */
    private BigDecimal calculateEligibleSubtotal(Coupon coupon, List<CartItem> items,
            Map<String, String> productCategoryMap) {
        List<String> scopeProducts = coupon.getAppliesToProducts();
        List<String> scopeCategories = coupon.getAppliesToCategories();

        boolean hasProductScope = scopeProducts != null && !scopeProducts.isEmpty();
        boolean hasCategoryScope = scopeCategories != null && !scopeCategories.isEmpty();

        if (!hasProductScope && !hasCategoryScope) {
            // No scope restrictions — all items are eligible
            return items.stream()
                    .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return items.stream()
                .filter(ci -> {
                    if (hasProductScope && scopeProducts.contains(ci.getProductId())) {
                        return true;
                    }
                    if (hasCategoryScope) {
                        String catId = productCategoryMap.get(ci.getProductId());
                        return catId != null && scopeCategories.contains(catId);
                    }
                    return false;
                })
                .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the discount for a coupon based on the given subtotal.
     * Mirrors the logic in CouponUseCaseImpl.calculateDiscount.
     */
    private BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        if (coupon.getType() == CouponType.FIXED) {
            return coupon.getValue().min(subtotal);
        }
        // FREE_SHIPPING — discount is 0, shipping will be zeroed separately
        return BigDecimal.ZERO;
    }
}
