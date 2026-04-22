package com.backandwhite.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CatalogPort;
import com.backandwhite.application.port.out.CatalogPort.ProductVerification;
import com.backandwhite.application.port.out.CmsPort;
import com.backandwhite.application.port.out.CmsPort.CampaignDiscountResult;
import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.application.usecase.CouponUseCase;
import com.backandwhite.application.usecase.InvoiceUseCase;
import com.backandwhite.application.usecase.ShippingTaxUseCase;
import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.common.exception.BusinessException;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.domain.model.Cart;
import com.backandwhite.domain.model.CartItem;
import com.backandwhite.domain.model.Coupon;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.model.OrderStats;
import com.backandwhite.domain.model.ShippingRule;
import com.backandwhite.domain.repository.CartRepository;
import com.backandwhite.domain.repository.OrderRepository;
import com.backandwhite.domain.valueobject.CouponType;
import com.backandwhite.domain.valueobject.OrderSagaStatus;
import com.backandwhite.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CouponUseCase couponUseCase;

    @Mock
    private ShippingTaxUseCase shippingTaxUseCase;

    @Mock
    private InvoiceUseCase invoiceUseCase;

    @Mock
    private CatalogPort catalogClient;

    @Mock
    private CmsPort cmsClient;

    @Mock
    private OrderEventPort orderEventPort;

    @Mock
    private com.backandwhite.application.service.InvoicePdfUrlSigner invoicePdfUrlSigner;

    @InjectMocks
    private OrderUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "storeUrl", "http://localhost:9000");
    }

    private Map<String, Object> validAddress() {
        Map<String, Object> a = new HashMap<>();
        a.put("country", "US");
        a.put("region", "CA");
        a.put("fullName", "John Doe");
        a.put("phone", "555-1234");
        a.put("street", "123 Main");
        a.put("city", "LA");
        a.put("postalCode", "90001");
        return a;
    }

    private CartItem sampleItem() {
        return CartItem.builder().productId("p1").variantId("v1").quantity(2).productName("Prod")
                .unitPrice(Money.of(new BigDecimal("10.00"))).build();
    }

    private Cart sampleCart() {
        return Cart.builder().id("c1").userId("u1").items(new ArrayList<>(List.of(sampleItem()))).build();
    }

    @Test
    void createFromCart_happyPath_savesOrder() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), new BigDecimal("5.00"), "cat1",
                        new BigDecimal("0.5"))));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(eq("US"), eq("CA"), any())).thenReturn(Money.of(new BigDecimal("2.00")));
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("o1");
            return o;
        });

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                "Some notes", "USD", null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("o1");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
        verify(cartRepository).update(cart);
    }

    @Test
    void createFromCart_missingShippingAddress_throws() {
        assertThatThrownBy(
                () -> useCase.createFromCart("u1", null, null, null, "card", null, null, null, null, null, null, "USD", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_emptyAddress_throws() {
        assertThatThrownBy(() -> useCase.createFromCart("u1", null, Map.of(), null, "card", null, null, null, null,
                null, null, "USD", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_noActiveCart_throws() {
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        when(cartRepository.findActiveBySessionId(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null,
                null, null, null, "USD", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_emptyCartItems_throws() {
        Cart cart = Cart.builder().id("c1").items(new ArrayList<>()).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null,
                null, null, null, "USD", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_insufficientStock_throws() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), new BigDecimal("5.00"), "cat1",
                        new BigDecimal("0.5"))));
        when(catalogClient.getAvailableStock("v1")).thenReturn(0);

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null,
                null, null, null, "USD", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_withSessionId_resolvesCartFromSession() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId(null)).thenReturn(Optional.empty());
        when(cartRepository.findActiveBySessionId("s1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart(null, "s1", validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null);
        assertThat(result).isNotNull();
    }

    @Test
    void createFromCart_withShippingOptions_appliesRate() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), new BigDecimal("5.00"), "cat1",
                        new BigDecimal("0.5"))));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        ShippingRule rule = ShippingRule.builder().rate(Money.of(new BigDecimal("5.00"))).build();
        when(shippingTaxUseCase.findShippingOptions(eq("US"), any(), any())).thenReturn(List.of(rule));
        when(shippingTaxUseCase.calculateTax(eq("US"), eq("CA"), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null);
        assertThat(result.getShippingCost().getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void createFromCart_withFreeShippingCampaign_zeroShipping() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1")).thenReturn(
                Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", new BigDecimal("0.5"))));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        ShippingRule rule = ShippingRule.builder().rate(Money.of(new BigDecimal("10.00"))).build();
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of(rule));
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null);
        assertThat(result.getShippingCost().isZero()).isTrue();
    }

    @Test
    void createFromCart_withPercentageCoupon_appliesDiscount() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);

        Coupon coupon = Coupon.builder().id("c1").code("SAVE10").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).active(true).build();
        when(couponUseCase.findByCode("SAVE10")).thenReturn(coupon);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "SAVE10", null, null, null,
                null, null, "USD", null);
        assertThat(result.getDiscountAmount().getAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    void createFromCart_withFreeShippingCoupon_zeroShipping() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        ShippingRule rule = ShippingRule.builder().rate(Money.of(new BigDecimal("10.00"))).build();
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of(rule));
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());

        Coupon coupon = Coupon.builder().id("c1").code("FREESHIP").type(CouponType.FREE_SHIPPING).value(Money.zero())
                .active(true).build();
        when(couponUseCase.findByCode("FREESHIP")).thenReturn(coupon);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "FREESHIP", null, null, null,
                null, null, "USD", null);
        assertThat(result.getShippingCost().isZero()).isTrue();
    }

    @Test
    void createFromCart_scopeRestrictedCouponNoMatch_throws() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());

        Coupon coupon = Coupon.builder().id("c1").code("SCOPED").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).active(true).appliesToProducts(List.of("other-product")).build();
        when(couponUseCase.findByCode("SCOPED")).thenReturn(coupon);

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, validAddress(), null, "card", "SCOPED", null, null,
                null, null, null, "USD", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_withFixedCoupon_appliesCappedDiscount() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);

        Coupon coupon = Coupon.builder().id("c1").code("FIX5").type(CouponType.FIXED)
                .value(Money.of(new BigDecimal("500"))).active(true).build();
        when(couponUseCase.findByCode("FIX5")).thenReturn(coupon);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "FIX5", null, null, null, null,
                null, "USD", null);
        // Discount caps at subtotal = 20
        assertThat(result.getDiscountAmount().getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void createFromCart_scopedCategoryCoupon_applies() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);

        Coupon coupon = Coupon.builder().id("c1").code("CAT").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).active(true).appliesToCategories(List.of("cat1")).build();
        when(couponUseCase.findByCode("CAT")).thenReturn(coupon);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "CAT", null, null, null, null,
                null, "USD", null);
        assertThat(result.getDiscountAmount().isPositive()).isTrue();
    }

    @Test
    void createFromCart_nonUsdCurrency_converts() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(cmsClient.getExchangeRate("EUR")).thenReturn(new BigDecimal("0.90"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "EUR", null);
        assertThat(result.getCurrencyCode()).isEqualTo("EUR");
        assertThat(result.getSubtotal().getAmount()).isEqualByComparingTo("18.00");
    }

    @Test
    void createFromCart_billingAddressNull_defaultsToShipping() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> addr = validAddress();
        Order result = useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null, null,
                "USD", null);
        assertThat(result.getBillingAddress()).isEqualTo(addr);
    }

    @Test
    void createFromCart_cartItemWithoutVerification_skipsStockCheck() {
        Cart cart = sampleCart();
        cart.getItems().get(0).setVariantId(null);
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", null)).thenReturn(Optional.empty());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null);
        assertThat(result).isNotNull();
    }

    @Test
    void createFromCart_withCampaignDiscount_appliesCorrection() {
        Cart cart = sampleCart();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), new BigDecimal("5.00"), "cat1",
                        new BigDecimal("0.5"))));
        when(catalogClient.getAvailableStock("v1")).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(new CampaignDiscountResult(Money.of(new BigDecimal("1.00")), "camp1"));
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null);
        assertThat(result.getCampaignDiscountTotal().isPositive()).isTrue();
    }

    // ── confirmOrder ────────────────────────────────────────
    @Test
    void confirmOrder_draft_transitionsToPending() {
        Map<String, Object> addr = validAddress();
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(addr)
                .items(List.of(OrderItem.builder().productId("p1").variantId("v1").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("10.00"))).totalPrice(Money.of(new BigDecimal("10.00")))
                        .productName("Prod").sku("SKU1").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.confirmOrder("o1", "u1", "test@example.com");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderEventPort).publishOrderCreated(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), any());
        verify(orderEventPort).publishStockDeducted(anyString(), anyString(), anyString(), anyInt());
        verify(invoiceUseCase).create(any());
    }

    @Test
    void confirmOrder_alreadyPending_idempotentReturn() {
        // Async Kafka consumer may have already advanced the order to PENDING
        // by the time the frontend hits confirmOrder. The call must be
        // idempotent — return the current order, do NOT throw.
        Order order = Order.builder().id("o1").status(OrderStatus.PENDING).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        Order result = useCase.confirmOrder("o1", "u1", "e@x");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void confirmOrder_cancelled_throws() {
        Order order = Order.builder().id("o1").status(OrderStatus.CANCELLED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> useCase.confirmOrder("o1", "u1", "e@x")).isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmOrder_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.confirmOrder("x", "u", "e")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void confirmOrder_withCoupon_appliesToOrder() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .couponId("coupon1").subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero())
                .taxAmount(Money.zero()).total(Money.of(new BigDecimal("20.00"))).currencyCode("USD")
                .shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.confirmOrder("o1", "u1", "e@x.com");

        verify(couponUseCase).applyCouponToOrder("coupon1", "u1", "o1");
    }

    @Test
    void confirmOrder_invoiceFails_swallowed() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List
                        .of(OrderItem.builder().productId("p").quantity(1).unitPrice(Money.of(new BigDecimal("20.00")))
                                .totalPrice(Money.of(new BigDecimal("20.00"))).productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("invoice boom")).when(invoiceUseCase).create(any());

        Order result = useCase.confirmOrder("o1", "u1", "e@x.com");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void confirmOrder_emailPublishFails_swallowed() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("email boom")).when(orderEventPort).publishInvoiceEmail(anyString(), anyString(),
                anyString(), any());

        Order result = useCase.confirmOrder("o1", "u1", "e@x.com");
        assertThat(result).isNotNull();
    }

    @Test
    void confirmOrder_emptyEmail_skipsEmailPublish() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.confirmOrder("o1", "u1", "");

        verify(orderEventPort, never()).publishInvoiceEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    void confirmOrder_itemWithoutVariant_skipsStockDeduct() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId(null).quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.confirmOrder("o1", "u1", "e@x.com");
        verify(orderEventPort, never()).publishStockDeducted(anyString(), anyString(), anyString(), anyInt());
    }

    // ── findById / findByOrderNumber ─────────────────────────
    @Test
    void findById_existing_returns() {
        Order o = Order.builder().id("o1").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
        assertThat(useCase.findById("o1")).isSameAs(o);
    }

    @Test
    void findById_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findById("x")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByOrderNumber_existing_returns() {
        Order o = Order.builder().id("o1").orderNumber("NX-1").build();
        when(orderRepository.findByOrderNumber("NX-1")).thenReturn(Optional.of(o));
        assertThat(useCase.findByOrderNumber("NX-1")).isSameAs(o);
    }

    @Test
    void findByOrderNumber_missing_throws() {
        when(orderRepository.findByOrderNumber("NX-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.findByOrderNumber("NX-X")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_asc() {
        Page<Order> page = new PageImpl<>(List.of(Order.builder().id("o1").build()));
        when(orderRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAll(Map.of(), 0, 10, "createdAt", true).content()).hasSize(1);
    }

    @Test
    void findAll_desc() {
        Page<Order> page = new PageImpl<>(List.of());
        when(orderRepository.findAll(any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findAll(Map.of(), 0, 10, "createdAt", false).content()).isEmpty();
    }

    @Test
    void findByUserId_asc() {
        Page<Order> page = new PageImpl<>(List.of(Order.builder().id("o1").build()));
        when(orderRepository.findByUserId(anyString(), any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findByUserId("u1", Map.of(), 0, 10, "createdAt", true).content()).hasSize(1);
    }

    @Test
    void findByUserId_desc() {
        Page<Order> page = new PageImpl<>(List.of());
        when(orderRepository.findByUserId(anyString(), any(), any(Pageable.class))).thenReturn(page);
        assertThat(useCase.findByUserId("u1", Map.of(), 0, 10, "createdAt", false).content()).isEmpty();
    }

    // ── updateStatus ─────────────────────────────────────────
    @Test
    void updateStatus_valid_transitions() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.updateStatus("o1", OrderStatus.PENDING, "admin", "Reason");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void updateStatus_invalidTransition_throws() {
        Order order = Order.builder().id("o1").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> useCase.updateStatus("o1", OrderStatus.PENDING, "admin", "No"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatus_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateStatus("x", OrderStatus.PENDING, "a", "r"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateStatus_delivered_publishesDeliveredEvent() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.IN_TRANSIT)
                .total(Money.of(new BigDecimal("100.00"))).currencyCode("USD").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("o1", OrderStatus.DELIVERED, "admin", "Delivered");

        verify(orderEventPort).publishOrderDelivered(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void updateStatus_deliveredWithEurCurrency_convertsToUsd() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.IN_TRANSIT)
                .total(Money.of(new BigDecimal("100.00"))).currencyCode("EUR").exchangeRateToUsd(new BigDecimal("1.1"))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("o1", OrderStatus.DELIVERED, "admin", "Delivered");
        verify(orderEventPort).publishOrderDelivered(anyString(), anyString(), any(), anyString(), eq("110.00"));
    }

    @Test
    void updateStatus_confirmed_publishesConfirmedWithUsdTotal() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.PENDING)
                .total(Money.of(new BigDecimal("100.00"))).currencyCode("EUR").exchangeRateToUsd(new BigDecimal("1.1"))
                .items(java.util.List.of()).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("o1", OrderStatus.CONFIRMED, "SYSTEM", "Payment confirmed");

        verify(orderEventPort).publishOrderConfirmed(anyString(), anyString(), any(), anyString(), eq("100.00"),
                eq("EUR"), eq("110.00"), anyInt());
    }

    // ── cancel ───────────────────────────────────────────────
    @Test
    void cancel_valid_cancels() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.cancel("o1", "u1", "Changed mind");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPort).publishOrderCancelled(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void cancel_nullReason_defaults() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.cancel("o1", "u1", null);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_invalidTransition_throws() {
        Order order = Order.builder().id("o1").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> useCase.cancel("o1", "u1", "r")).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.cancel("x", "u", "r")).isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateSagaStatus ─────────────────────────────────────
    @Test
    void updateSagaStatus_existing() {
        Order order = Order.builder().id("o1").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.updateSagaStatus("o1", OrderSagaStatus.PAYMENT_CONFIRMED);
        assertThat(result.getSagaStatus()).isEqualTo(OrderSagaStatus.PAYMENT_CONFIRMED);
    }

    @Test
    void updateSagaStatus_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateSagaStatus("x", OrderSagaStatus.PAYMENT_CONFIRMED))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateCjFields ───────────────────────────────────────
    @Test
    void updateCjFields_updatesBoth() {
        Order order = Order.builder().id("o1").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.updateCjFields("o1", "CJ1", "TR1");
        assertThat(result.getCjOrderId()).isEqualTo("CJ1");
        assertThat(result.getTrackNumber()).isEqualTo("TR1");
    }

    @Test
    void updateCjFields_nullsIgnored() {
        Order order = Order.builder().id("o1").cjOrderId("EXISTING").trackNumber("OLDTRACK").build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.updateCjFields("o1", null, null);
        assertThat(result.getCjOrderId()).isEqualTo("EXISTING");
        assertThat(result.getTrackNumber()).isEqualTo("OLDTRACK");
    }

    @Test
    void updateCjFields_missing_throws() {
        when(orderRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.updateCjFields("x", null, null)).isInstanceOf(EntityNotFoundException.class);
    }

    // ── getStats ─────────────────────────────────────────────
    @Test
    void getStats_delegatesToRepository() {
        OrderStats stats = OrderStats.builder().totalOrders(5).build();
        when(orderRepository.getStats()).thenReturn(stats);
        assertThat(useCase.getStats()).isSameAs(stats);
    }
}
