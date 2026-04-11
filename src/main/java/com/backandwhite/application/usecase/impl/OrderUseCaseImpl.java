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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backandwhite.common.domain.valueobject.Money;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.backandwhite.common.exception.Message.ENTITY_NOT_FOUND;
import static com.backandwhite.domain.exception.Message.*;

@Log4j2
@Service
public class OrderUseCaseImpl implements OrderUseCase {

    @Value("${app.store.url:http://localhost:9000}")
    private String storeUrl;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CouponUseCase couponUseCase;
    private final ShippingTaxUseCase shippingTaxUseCase;
    private final InvoiceUseCase invoiceUseCase;
    private final CatalogPort catalogClient;
    private final CmsPort cmsClient;
    private final OrderEventPort orderEventPort;

    public OrderUseCaseImpl(OrderRepository orderRepository, CartRepository cartRepository,
            CouponUseCase couponUseCase, ShippingTaxUseCase shippingTaxUseCase,
            InvoiceUseCase invoiceUseCase, CatalogPort catalogClient,
            CmsPort cmsClient, OrderEventPort orderEventPort) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.couponUseCase = couponUseCase;
        this.shippingTaxUseCase = shippingTaxUseCase;
        this.invoiceUseCase = invoiceUseCase;
        this.catalogClient = catalogClient;
        this.cmsClient = cmsClient;
        this.orderEventPort = orderEventPort;
    }

    @Override
    @Transactional
    public Order createFromCart(String userId, String sessionId,
            Map<String, Object> shippingAddress,
            Map<String, Object> billingAddress,
            String paymentMethod, String couponCode,
            String giftCardCode, BigDecimal giftCardAmount,
            Integer loyaltyPointsUsed, BigDecimal loyaltyDiscount,
            String notes, String currencyCode) {

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
                BigDecimal costPriceRaw = verification.get().costPrice();
                Money basePriceMoney = Money.of(basePrice);
                Money costPriceMoney = Money.of(costPriceRaw != null ? costPriceRaw : basePrice);
                String categoryId = verification.get().categoryId();
                BigDecimal itemWeight = verification.get().weight();
                productCategoryMap.put(ci.getProductId(), categoryId);

                // Accumulate weight (M-03)
                if (itemWeight != null && itemWeight.compareTo(BigDecimal.ZERO) > 0) {
                    totalWeight = totalWeight.add(
                            itemWeight.multiply(BigDecimal.valueOf(ci.getQuantity())));
                }

                // Apply best campaign discount on MARGIN ONLY (server-side — C-02)
                Money campaignDiscount = cmsClient.calculateBestCampaignDiscount(
                        activeCampaigns, ci.getProductId(), categoryId, basePriceMoney, costPriceMoney);
                Money verifiedPrice = basePriceMoney.subtract(campaignDiscount).floor();

                if (!ci.getUnitPrice().equals(verifiedPrice)) {
                    log.warn(
                            "Price correction: product={}, variant={}, cart={}, verified={} (base={}, campaign discount={})",
                            ci.getProductId(), ci.getVariantId(),
                            ci.getUnitPrice().toPlainString(), verifiedPrice.toPlainString(),
                            basePriceMoney.toPlainString(), campaignDiscount.toPlainString());
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
        Money subtotal = cart.getItems().stream()
                .map(ci -> ci.getUnitPrice().multiply(ci.getQuantity()))
                .reduce(Money.zero(), Money::add);

        // 2. Calculate shipping (using real product weight — M-03)
        String country = shippingAddress.getOrDefault("country", "").toString();
        Money shippingCost = Money.zero();
        List<ShippingRule> options = shippingTaxUseCase.findShippingOptions(country, totalWeight, subtotal);
        if (!options.isEmpty()) {
            shippingCost = options.getFirst().getRate();
        }

        // 3. Calculate tax
        String region = shippingAddress.getOrDefault("region", "").toString();
        Money taxAmount = shippingTaxUseCase.calculateTax(country, region, subtotal);

        // 4. Apply coupon (with scope filtering for appliesToProducts/Categories —
        // M-05)
        Money discountAmount = Money.zero();
        String couponId = null;
        boolean freeShipping = false;

        if (couponCode != null && !couponCode.isBlank()) {
            // Basic validation (active, not expired, usage limits, min order)
            couponUseCase.validate(couponCode, subtotal, userId);

            Coupon coupon = couponUseCase.findByCode(couponCode);
            couponId = coupon.getId();
            freeShipping = coupon.getType() == CouponType.FREE_SHIPPING;

            // Check coupon scope and calculate eligible subtotal (M-05)
            Money eligibleSubtotal = calculateEligibleSubtotal(
                    coupon, cart.getItems(), productCategoryMap);

            boolean hasScopeRestrictions = (coupon.getAppliesToProducts() != null
                    && !coupon.getAppliesToProducts().isEmpty())
                    || (coupon.getAppliesToCategories() != null && !coupon.getAppliesToCategories().isEmpty());

            if (hasScopeRestrictions && eligibleSubtotal.isZero()) {
                throw COUPON_SCOPE_MISMATCH.toBusinessException();
            }

            // Calculate discount on eligible subtotal only
            Money discountBase = hasScopeRestrictions ? eligibleSubtotal : subtotal;
            discountAmount = calculateCouponDiscount(coupon, discountBase);
        }

        if (freeShipping) {
            shippingCost = Money.zero();
        }

        Money total = subtotal.add(shippingCost).add(taxAmount).subtract(discountAmount).floor();

        // 4.5 Multi-currency: fetch exchange rate and convert totals
        // NOTE: Price verification (step 1.5) already corrected cart prices to USD.
        // We must convert USD → target currency here (single conversion).
        String resolvedCurrency = (currencyCode != null && !currencyCode.isBlank()) ? currencyCode.toUpperCase()
                : "USD";
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal exchangeRateToUsd = BigDecimal.ONE;

        if (!"USD".equals(resolvedCurrency)) {
            exchangeRate = cmsClient.getExchangeRate(resolvedCurrency);
            if (exchangeRate.compareTo(BigDecimal.ZERO) > 0 && exchangeRate.compareTo(BigDecimal.ONE) != 0) {
                exchangeRateToUsd = BigDecimal.ONE.divide(exchangeRate, 8, java.math.RoundingMode.HALF_UP);
                subtotal = subtotal.multiply(exchangeRate);
                shippingCost = shippingCost.multiply(exchangeRate);
                taxAmount = taxAmount.multiply(exchangeRate);
                discountAmount = discountAmount.multiply(exchangeRate);
                total = subtotal.add(shippingCost).add(taxAmount).subtract(discountAmount).floor();
            }
        }

        // 5. Build order items from cart items
        // Cart prices were corrected to USD in step 1.5; now convert to target
        // currency.
        final BigDecimal fxRate = exchangeRate;
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(ci -> {
                    // ci.getUnitPrice() is in USD (corrected by price verification)
                    Money up = ci.getUnitPrice().multiply(fxRate);
                    return OrderItem.builder()
                            .productId(ci.getProductId())
                            .variantId(ci.getVariantId())
                            .sku(null)
                            .productName(ci.getProductName())
                            .productImage(ci.getProductImage())
                            .quantity(ci.getQuantity())
                            .unitPrice(up)
                            .totalPrice(up.multiply(ci.getQuantity()))
                            .build();
                })
                .toList();

        // 6. Build order
        Money gcAmount = Money.of(giftCardAmount);
        Money lyDiscount = Money.of(loyaltyDiscount);
        int lyPoints = loyaltyPointsUsed != null ? loyaltyPointsUsed : 0;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .status(OrderStatus.DRAFT)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .total(total)
                .currencyCode(resolvedCurrency)
                .exchangeRateToUsd(exchangeRateToUsd)
                .couponId(couponId)
                .giftCardCode(giftCardCode)
                .giftCardAmount(gcAmount)
                .loyaltyPointsUsed(lyPoints)
                .loyaltyDiscount(lyDiscount)
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
    public Order confirmOrder(String orderId, String userId, String email) {
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
                confirmed.getId(), userId, email, confirmed.getOrderNumber(),
                confirmed.getTotal().toPlainString(),
                confirmed.getCurrencyCode() != null ? confirmed.getCurrencyCode() : "USD",
                confirmed.getStatus().name(),
                order.getItems().size(), null);

        // Deduct stock for each item via Kafka
        for (OrderItem oi : order.getItems()) {
            if (oi.getVariantId() != null && !oi.getVariantId().isBlank()) {
                orderEventPort.publishStockDeducted(
                        oi.getProductId(),
                        oi.getVariantId(),
                        confirmed.getId(),
                        oi.getQuantity());
            }
        }

        // Auto-create invoice
        Invoice invoice = null;
        try {
            Map<String, Object> customerSnapshot = new LinkedHashMap<>();
            customerSnapshot.put("name", confirmed.getShippingAddress().getOrDefault("fullName", ""));
            customerSnapshot.put("phone", confirmed.getShippingAddress().getOrDefault("phone", ""));
            customerSnapshot.put("address", buildAddressString(confirmed.getShippingAddress()));

            List<Map<String, Object>> invoiceLines = new ArrayList<>();
            for (OrderItem oi : order.getItems()) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("name", oi.getProductName());
                line.put("sku", oi.getSku());
                line.put("quantity", oi.getQuantity());
                line.put("unitPrice", oi.getUnitPrice().getAmount());
                line.put("total", oi.getTotalPrice().getAmount());
                invoiceLines.add(line);
            }

            invoice = Invoice.builder()
                    .invoiceNumber(generateInvoiceNumber())
                    .orderId(confirmed.getId())
                    .status(InvoiceStatus.PAID)
                    .issueDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(30))
                    .subtotal(confirmed.getSubtotal())
                    .shipping(confirmed.getShippingCost())
                    .tax(confirmed.getTaxAmount())
                    .total(confirmed.getTotal())
                    .discountAmount(
                            confirmed.getDiscountAmount() != null ? confirmed.getDiscountAmount() : Money.zero())
                    .giftCardAmount(
                            confirmed.getGiftCardAmount() != null ? confirmed.getGiftCardAmount() : Money.zero())
                    .loyaltyDiscount(
                            confirmed.getLoyaltyDiscount() != null ? confirmed.getLoyaltyDiscount() : Money.zero())
                    .paymentMethod(confirmed.getPaymentMethod())
                    .currencyCode(confirmed.getCurrencyCode() != null ? confirmed.getCurrencyCode() : "USD")
                    .customerSnapshot(customerSnapshot)
                    .lines(invoiceLines)
                    .notes(null)
                    .build();

            invoiceUseCase.create(invoice);
            log.info("Invoice {} created for order {}", invoice.getInvoiceNumber(), confirmed.getOrderNumber());
        } catch (Exception e) {
            log.warn("Failed to auto-create invoice for order {}: {}", confirmed.getId(), e.getMessage());
        }

        // Send invoice email via Kafka → notification service
        if (invoice != null && email != null && !email.isBlank()) {
            try {
                orderEventPort.publishInvoiceEmail(
                        email,
                        "Factura de tu pedido " + confirmed.getOrderNumber(),
                        "order-invoice",
                        buildInvoiceEmailVars(confirmed, invoice));
                log.info("Invoice email event published for order {} to {}", confirmed.getOrderNumber(), email);
            } catch (Exception e) {
                log.warn("Failed to publish invoice email for order {}: {}", confirmed.getId(), e.getMessage());
            }
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

        // When order is delivered, publish specific delivery event for loyalty
        // processing — always send the total converted to USD
        if (newStatus == OrderStatus.DELIVERED) {
            BigDecimal totalUsd = updated.getTotal().getAmount();
            if (updated.getExchangeRateToUsd() != null
                    && updated.getExchangeRateToUsd().compareTo(BigDecimal.ZERO) > 0
                    && !"USD".equalsIgnoreCase(updated.getCurrencyCode())) {
                totalUsd = updated.getTotal().getAmount()
                        .multiply(updated.getExchangeRateToUsd())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            orderEventPort.publishOrderDelivered(
                    updated.getId(),
                    updated.getUserId(),
                    null,
                    updated.getOrderNumber(),
                    totalUsd.toPlainString());
            log.info("::> Published order.delivered event for order={}, userId={}, totalLocal={} {}, totalUsd={}",
                    updated.getOrderNumber(), updated.getUserId(),
                    updated.getTotal().toPlainString(), updated.getCurrencyCode(),
                    totalUsd.toPlainString());
        }

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
    private Money calculateEligibleSubtotal(Coupon coupon, List<CartItem> items,
            Map<String, String> productCategoryMap) {
        List<String> scopeProducts = coupon.getAppliesToProducts();
        List<String> scopeCategories = coupon.getAppliesToCategories();

        boolean hasProductScope = scopeProducts != null && !scopeProducts.isEmpty();
        boolean hasCategoryScope = scopeCategories != null && !scopeCategories.isEmpty();

        if (!hasProductScope && !hasCategoryScope) {
            // No scope restrictions — all items are eligible
            return items.stream()
                    .map(ci -> ci.getUnitPrice().multiply(ci.getQuantity()))
                    .reduce(Money.zero(), Money::add);
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
                .map(ci -> ci.getUnitPrice().multiply(ci.getQuantity()))
                .reduce(Money.zero(), Money::add);
    }

    /**
     * Calculates the discount for a coupon based on the given subtotal.
     * Mirrors the logic in CouponUseCaseImpl.calculateDiscount.
     */
    private Money calculateCouponDiscount(Coupon coupon, Money subtotal) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return subtotal.percentage(coupon.getValue().getAmount());
        }
        if (coupon.getType() == CouponType.FIXED) {
            return coupon.getValue().min(subtotal);
        }
        // FREE_SHIPPING — discount is 0, shipping will be zeroed separately
        return Money.zero();
    }

    /**
     * Builds the template variables map for the invoice email notification.
     * All values are strings (as required by the Avro EmailNotificationEvent
     * schema).
     */
    private Map<String, String> buildInvoiceEmailVars(Order order, Invoice invoice) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("invoiceNumber", invoice.getInvoiceNumber());
        vars.put("orderNumber", order.getOrderNumber());
        vars.put("issueDate", invoice.getIssueDate().toString());
        vars.put("paymentMethod", fmtPaymentMethod(order.getPaymentMethod()));

        // Customer snapshot
        Map<String, Object> cs = invoice.getCustomerSnapshot();
        vars.put("customerName", cs != null ? String.valueOf(cs.getOrDefault("name", "")) : "");
        vars.put("customerPhone", cs != null ? String.valueOf(cs.getOrDefault("phone", "")) : "");
        vars.put("customerAddress", cs != null ? String.valueOf(cs.getOrDefault("address", "")) : "");

        // Pre-render line items as HTML table rows (avoids Thymeleaf preprocessing
        // complexity)
        List<Map<String, Object>> lines = invoice.getLines();
        vars.put("itemCount", String.valueOf(lines != null ? lines.size() : 0));
        vars.put("linesHtml", buildLinesHtml(lines, order.getCurrencyCode() != null ? order.getCurrencyCode() : "USD"));

        // Totals
        vars.put("subtotal", fmt(invoice.getSubtotal()));
        vars.put("shipping", fmt(invoice.getShipping()));
        vars.put("tax", fmt(invoice.getTax()));
        vars.put("discount", fmt(invoice.getDiscountAmount()));
        vars.put("giftCard", fmt(invoice.getGiftCardAmount()));
        vars.put("loyaltyDiscount", fmt(invoice.getLoyaltyDiscount()));
        vars.put("total", fmt(invoice.getTotal()));
        vars.put("currency", order.getCurrencyCode() != null ? order.getCurrencyCode() : "USD");

        // Invoice download URL & QR code
        String invoiceUrl = storeUrl + "/api/v1/invoices/order/" + order.getId() + "/pdf";
        vars.put("invoiceUrl", invoiceUrl);
        vars.put("qrCodeUrl", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data="
                + URLEncoder.encode(invoiceUrl, StandardCharsets.UTF_8));

        return vars;
    }

    /**
     * Builds the HTML for invoice line item rows to be injected via th:utext.
     */
    private String buildLinesHtml(List<Map<String, Object>> lines, String currency) {
        if (lines == null || lines.isEmpty())
            return "";
        var sb = new StringBuilder();
        for (Map<String, Object> l : lines) {
            String name = String.valueOf(l.getOrDefault("name", ""));
            String sku = String.valueOf(l.getOrDefault("sku", ""));
            String qty = String.valueOf(l.getOrDefault("quantity", "0"));
            String price = fmt(l.get("unitPrice"));
            String total = fmt(l.get("total"));

            sb.append("<tr><td style=\"background:#ffffff;padding:0 40px;\">")
                    .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-bottom:1px solid #f1f5f9;\">")
                    .append("<tr>")
                    .append("<td style=\"padding:12px 0;\">")
                    .append("<p style=\"margin:0 0 2px;font-size:14px;color:#1e293b;font-weight:500;\">")
                    .append(escHtml(name)).append("</p>")
                    .append("<p style=\"margin:0;font-size:11px;color:#94a3b8;font-family:'Courier New',monospace;\">")
                    .append(escHtml(sku)).append("</p>")
                    .append("</td>")
                    .append("<td align=\"center\" style=\"padding:12px 0;font-size:14px;color:#334155;width:50px;\">")
                    .append(escHtml(qty)).append("</td>")
                    .append("<td align=\"right\" style=\"padding:12px 0;font-size:14px;color:#334155;width:80px;\">")
                    .append(escHtml(price)).append(" <span style=\"font-size:11px;color:#94a3b8;\">")
                    .append(escHtml(currency)).append("</span></td>")
                    .append("<td align=\"right\" style=\"padding:12px 0;font-size:14px;color:#1e293b;font-weight:500;width:80px;\">")
                    .append(escHtml(total)).append(" <span style=\"font-size:11px;color:#94a3b8;\">")
                    .append(escHtml(currency)).append("</span></td>")
                    .append("</tr></table></td></tr>");
        }
        return sb.toString();
    }

    private String escHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String fmt(Object value) {
        if (value == null)
            return "0.00";
        if (value instanceof Money m)
            return m.toPlainString();
        if (value instanceof BigDecimal bd)
            return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        return value.toString();
    }

    private String fmtPaymentMethod(String pm) {
        if (pm == null)
            return "—";
        return switch (pm.toLowerCase()) {
            case "card", "credit_card", "creditcard" -> "Tarjeta de crédito";
            case "debit_card", "debitcard" -> "Tarjeta de débito";
            case "paypal" -> "PayPal";
            case "usdt", "crypto_usdt" -> "USDT (Crypto)";
            case "btc", "crypto_btc" -> "Bitcoin (Crypto)";
            case "bank_transfer", "banktransfer" -> "Transferencia bancaria";
            case "cash_on_delivery", "cashondelivery", "cod" -> "Contra reembolso";
            case "gift_card" -> "Tarjeta de regalo";
            case "none" -> "Sin cargo";
            default -> pm;
        };
    }
}
