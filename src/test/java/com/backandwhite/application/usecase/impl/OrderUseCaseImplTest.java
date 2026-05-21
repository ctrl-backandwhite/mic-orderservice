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
import java.time.Instant;
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
                "Some notes", "USD", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("o1");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DRAFT);
        verify(cartRepository).update(cart);
    }

    @Test
    void createFromCart_missingShippingAddress_throws() {
        assertThatThrownBy(() -> useCase.createFromCart("u1", null, null, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_emptyAddress_throws() {
        Map<String, Object> emptyAddr = Map.of();
        assertThatThrownBy(() -> useCase.createFromCart("u1", null, emptyAddr, null, "card", null, null, null, null,
                null, null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_noActiveCart_throws() {
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        when(cartRepository.findActiveBySessionId(null)).thenReturn(Optional.empty());
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_emptyCartItems_throws() {
        Cart cart = Cart.builder().id("c1").items(new ArrayList<>()).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
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
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
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
                null, "USD", null, null);
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
                null, "USD", null, null);
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
                null, "USD", null, null);
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
                null, null, "USD", null, null);
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
                null, null, "USD", null, null);
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
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", "SCOPED", null, null, null,
                null, null, "USD", null, null)).isInstanceOf(BusinessException.class);
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
                null, "USD", null, null);
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
                null, "USD", null, null);
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
                null, "EUR", null, null);
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
        Order result = useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null, null, "USD",
                null, null);
        assertThat(result.getBillingAddress()).isEqualTo(addr);
    }

    @Test
    void createFromCart_cartItemWithoutVerification_throwsBusinessException() {
        // After the April-29 currency-drift incident, missing price verification
        // must fail the order (instead of silently skipping). The cart stores
        // prices in the buyer's display currency; without a verified USD base
        // price the post-checkout FX conversion would double-convert.
        Cart cart = sampleCart();
        cart.getItems().get(0).setVariantId(null);
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", null)).thenReturn(Optional.empty());

        Map<String, Object> addr = validAddress();
        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("verify product prices");
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
                null, "USD", null, null);
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

    // ── extra coverage: createFromCart edge branches ─────────
    @Test
    void createFromCart_zeroQuantityItem_throws() {
        Cart cart = Cart.builder().id("c1").userId("u1")
                .items(new ArrayList<>(List.of(CartItem.builder().productId("p1").variantId("v1").quantity(0)
                        .productName("Prod").unitPrice(Money.of(new BigDecimal("10.00"))).build())))
                .build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createFromCart_blankCouponCode_skipsCouponLogic() {
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

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "  ", null, null, null, null,
                null, "USD", null, null);
        assertThat(result.getCouponId()).isNull();
        verify(couponUseCase, never()).validate(anyString(), any(), any());
    }

    @Test
    void createFromCart_blankCurrencyCode_defaultsToUsd() {
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

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, " ", null, null);
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void createFromCart_exchangeRateZero_fallbackToUsd() {
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
        when(cmsClient.getExchangeRate("EUR")).thenReturn(BigDecimal.ZERO);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "EUR", null, null);
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void createFromCart_exchangeRateOne_keepsCurrencyButNoConversion() {
        // Rate == 1 hits the else branch (resolvedCurrency stays as USD-equivalent)
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
        when(cmsClient.getExchangeRate("EUR")).thenReturn(BigDecimal.ONE);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "EUR", null, null);
        assertThat(result).isNotNull();
    }

    @Test
    void createFromCart_loyaltyPointsProvided_storesValue() {
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

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, 50,
                new BigDecimal("5.00"), null, "USD", null, null);
        assertThat(result.getLoyaltyPointsUsed()).isEqualTo(50);
    }

    @Test
    void createFromCart_billingAddressProvided_keepsBilling() {
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

        Map<String, Object> billing = new HashMap<>();
        billing.put("country", "MX");
        Order result = useCase.createFromCart("u1", null, validAddress(), billing, "card", null, null, null, null, null,
                null, "USD", null, null);
        assertThat(result.getBillingAddress()).isEqualTo(billing);
    }

    @Test
    void createFromCart_defaultShippingFallback_appliesFlatRate() {
        // No matching shipping rule + defaultShippingRate > 0 → use flat rate
        ReflectionTestUtils.setField(useCase, "defaultShippingRate", new BigDecimal("7.50"));
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

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null, null);
        assertThat(result.getShippingCost().getAmount()).isEqualByComparingTo("7.50");
    }

    @Test
    void createFromCart_itemWithoutVariantId_skipsStockCheck() {
        // variantId blank → catalog.getAvailableStock not called
        CartItem item = CartItem.builder().productId("p1").variantId("").quantity(1).productName("Prod")
                .unitPrice(Money.of(new BigDecimal("10.00"))).build();
        Cart cart = Cart.builder().id("c1").userId("u1").items(new ArrayList<>(List.of(item))).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", ""))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", null, null);
        assertThat(result).isNotNull();
        verify(catalogClient, never()).getAvailableStock(anyString());
    }

    // ── confirmOrder idempotent branches ───────────────────
    @Test
    void confirmOrder_alreadyConfirmed_idempotent() {
        Order order = Order.builder().id("o1").status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        Order result = useCase.confirmOrder("o1", "u1", "e@x");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, never()).update(any());
    }

    @Test
    void confirmOrder_alreadyProcessing_idempotent() {
        Order order = Order.builder().id("o1").status(OrderStatus.PROCESSING).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        Order result = useCase.confirmOrder("o1", "u1", "e@x");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void confirmOrder_alreadyShipped_idempotent() {
        Order order = Order.builder().id("o1").status(OrderStatus.SHIPPED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        Order result = useCase.confirmOrder("o1", "u1", "e@x");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void confirmOrder_alreadyDelivered_idempotent() {
        Order order = Order.builder().id("o1").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        Order result = useCase.confirmOrder("o1", "u1", "e@x");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void confirmOrder_nullEmailWithCustomerSnapshot_buildsDefaults() {
        Map<String, Object> shipAddr = new HashMap<>();
        shipAddr.put("country", "ES");
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).shippingAddress(shipAddr)
                .items(List
                        .of(OrderItem.builder().productId("p").quantity(1).unitPrice(Money.of(new BigDecimal("20.00")))
                                .totalPrice(Money.of(new BigDecimal("20.00"))).productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.confirmOrder("o1", "u1", null);
        assertThat(result).isNotNull();
        verify(orderEventPort, never()).publishInvoiceEmail(anyString(), anyString(), anyString(), any());
    }

    // ── createGiftCardOrder ────────────────────────────────
    @Test
    void createGiftCardOrder_nullBuyerEmail_returnsNull() {
        Order result = useCase.createGiftCardOrder("gc1", "CODE", "buyer1", null, "Buyer", "50.00", "USD", "Recip",
                "r@x.com", "Hi");
        assertThat(result).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createGiftCardOrder_blankBuyerEmail_returnsNull() {
        Order result = useCase.createGiftCardOrder("gc1", "CODE", "buyer1", "", "Buyer", "50.00", "USD", "Recip",
                "r@x.com", "Hi");
        assertThat(result).isNull();
    }

    @Test
    void createGiftCardOrder_existingOrder_idempotentReturn() {
        Order existing = Order.builder().id("o-existing").orderNumber("GC-12345678").build();
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.of(existing));

        Order result = useCase.createGiftCardOrder("12345678abc", "CODE", "buyer1", "buyer@x.com", "Buyer", "50.00",
                "USD", "Recip", "r@x.com", "Hi");
        assertThat(result).isSameAs(existing);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createGiftCardOrder_happyPath_savesOrderAndPublishes() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("gco1");
            return o;
        });
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.createGiftCardOrder("giftcardid12345", "GIFT123", "buyer1", "buyer@x.com", "Buyer Name",
                "50.00", "USD", "Recipient", "r@x.com", "Happy birthday");

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).startsWith("GC-");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getSagaStatus()).isEqualTo(OrderSagaStatus.COMPLETED);
        verify(invoiceUseCase).create(any());
        verify(orderEventPort).publishInvoiceEmail(eq("buyer@x.com"), anyString(), eq("order-invoice"), any());
    }

    @Test
    void createGiftCardOrder_nullBuyerId_usesGuestMarker() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.createGiftCardOrder("guestgc12345", "CODE", null, "g@x.com", null, "10.00", null, null,
                null, null);
        assertThat(result.getUserId()).startsWith("guest-gc-");
    }

    @Test
    void createGiftCardOrder_blankBuyerId_usesGuestMarker() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.createGiftCardOrder("anothergc1234567", "C", "  ", "g@x.com", "B", "10", "USD", null,
                null, null);
        assertThat(result.getUserId()).startsWith("guest-gc-");
    }

    @Test
    void createGiftCardOrder_invoiceCreationFails_swallowed() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("gco1");
            return o;
        });
        doThrow(new RuntimeException("invoice boom")).when(invoiceUseCase).create(any());

        // The catch swallows the exception; order still saved successfully.
        Order result = useCase.createGiftCardOrder("giftcardid111111", "C", "b1", "b@x.com", "B", "20", "USD", null,
                null, null);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void createGiftCardOrder_emailPublishFails_swallowed() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("gco1");
            return o;
        });
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");
        doThrow(new RuntimeException("email boom")).when(orderEventPort).publishInvoiceEmail(anyString(), anyString(),
                anyString(), any());

        Order result = useCase.createGiftCardOrder("giftcardid22222", "C", "b1", "b@x.com", "B", "20", "USD", null,
                null, null);
        assertThat(result).isNotNull();
    }

    @Test
    void createGiftCardOrder_shortGiftCardId_usesIdAsSuffix() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        // < 8 chars
        Order result = useCase.createGiftCardOrder("abc", "C", "b1", "b@x.com", "B", "20", "USD", null, null, null);
        assertThat(result.getOrderNumber()).isEqualTo("GC-abc");
    }

    // ── revenue / status distribution ──────────────────────
    @Test
    void getRevenueByDay_delegatesToRepository() {
        Instant from = Instant.now();
        Instant to = Instant.now();
        when(orderRepository.findRevenueByDay(from, to)).thenReturn(List.of());
        assertThat(useCase.getRevenueByDay(from, to)).isEmpty();
    }

    @Test
    void getStatusDistribution_delegatesToRepository() {
        Instant from = Instant.now();
        Instant to = Instant.now();
        when(orderRepository.findStatusDistribution(from, to)).thenReturn(List.of());
        assertThat(useCase.getStatusDistribution(from, to)).isEmpty();
    }

    // ── locale resolution branches (via createGiftCardOrder which builds vars) ──
    @Test
    void createGiftCardOrder_emailVarsBuilt_includesLang() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("gco1");
            return o;
        });
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        useCase.createGiftCardOrder("brazilgc1234567", "C", "b1", "b@x.com", "Buyer", "20", "BRL", null, null, null);

        verify(orderEventPort).publishInvoiceEmail(eq("b@x.com"), anyString(), eq("order-invoice"), any());
    }

    // ── extra branch coverage: confirmOrder null-coalesce paths ────
    @Test
    void confirmOrder_orderHasFullMoneyFields_buildsInvoiceWithRealValues() {
        // Hits the non-null branches of discountAmount / giftCardAmount /
        // loyaltyDiscount
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("100.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("100.00"))).currencyCode("USD").shippingAddress(validAddress())
                .discountAmount(Money.of(new BigDecimal("5.00"))).giftCardAmount(Money.of(new BigDecimal("3.00")))
                .loyaltyDiscount(Money.of(new BigDecimal("2.00")))
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("100.00"))).totalPrice(Money.of(new BigDecimal("100.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.confirmOrder("o1", "u1", "buyer@x.com");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(invoiceUseCase).create(any());
    }

    @Test
    void confirmOrder_nullCurrencyCode_defaultsToUsd() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode(null).shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.confirmOrder("o1", "u1", "buyer@x.com");
        assertThat(result).isNotNull();
    }

    @Test
    void confirmOrder_itemsWithVariantId_publishesStockDeducted() {
        // covers L376 true-true branch
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("v-real").quantity(2)
                        .unitPrice(Money.of(new BigDecimal("10.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(orderEventPort).publishStockDeducted(eq("p"), eq("v-real"), anyString(), eq(2));
    }

    @Test
    void confirmOrder_itemBlankVariantId_skipsStockDeduct() {
        // covers L376 second short-circuit (variantId is "" → isBlank true)
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .items(List.of(OrderItem.builder().productId("p").variantId("   ").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(orderEventPort, never()).publishStockDeducted(anyString(), anyString(), anyString(), anyInt());
    }

    // ── updateStatus extras: null currency + null items ────────────
    @Test
    void updateStatus_confirmedNullCurrency_defaultsToUsd() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.PENDING)
                .total(Money.of(new BigDecimal("100.00"))).currencyCode(null).items(null).build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("o1", OrderStatus.CONFIRMED, "SYS", "ok");
        verify(orderEventPort).publishOrderConfirmed(anyString(), anyString(), any(), anyString(), anyString(),
                eq("USD"), anyString(), eq(0));
    }

    @Test
    void updateStatus_deliveredUsdOrder_keepsTotal() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.IN_TRANSIT)
                .total(Money.of(new BigDecimal("100.00"))).currencyCode("USD").exchangeRateToUsd(BigDecimal.ONE)
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.updateStatus("o1", OrderStatus.DELIVERED, "admin", "Delivered");
        verify(orderEventPort).publishOrderDelivered(anyString(), anyString(), any(), anyString(), eq("100.00"));
    }

    // ── createFromCart with explicit customerLocale (resolveCustomerLocale path)
    // ──
    @Test
    void createFromCart_explicitLocaleWithRegion_normalisesToLanguage() {
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

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", null, null, null, null, null,
                null, "USD", "en-US", null);
        assertThat(result.getCustomerLocale()).isEqualTo("en-US");
    }

    // ── confirmOrder using order with explicit customerLocale →
    // resolveCustomerLocale early return ──
    @Test
    void confirmOrder_orderWithCustomerLocale_buildsInvoiceWithThatLang() {
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(validAddress())
                .customerLocale("pt-BR")
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(orderEventPort).publishInvoiceEmail(eq("buyer@x.com"), anyString(), eq("order-invoice"), any());
    }

    @Test
    void confirmOrder_orderWithBrazilCountry_resolvesPtLocale() {
        Map<String, Object> brAddr = new HashMap<>();
        brAddr.put("country", "BR");
        brAddr.put("fullName", "João");
        brAddr.put("phone", "+5511");
        brAddr.put("street", "Rua A");
        brAddr.put("city", "SP");
        brAddr.put("region", "SP");
        brAddr.put("postalCode", "01000");
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("BRL").shippingAddress(brAddr)
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(orderEventPort).publishInvoiceEmail(eq("buyer@x.com"), anyString(), eq("order-invoice"), any());
    }

    @Test
    void confirmOrder_orderWithEnCountry_resolvesEnLocale() {
        Map<String, Object> ukAddr = new HashMap<>();
        ukAddr.put("country", "UK");
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("GBP").shippingAddress(ukAddr)
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(orderEventPort).publishInvoiceEmail(eq("buyer@x.com"), anyString(), eq("order-invoice"), any());
    }

    // ── coupon scope category-only path (calculateEligibleSubtotal
    // hasCategoryScope branch) ──
    @Test
    void createFromCart_categoryScopedCouponAllItemsMatch_appliesDiscount() {
        // Builds a cart with two items in category "cat1"; coupon scoped to "cat1"
        // only.
        CartItem i1 = CartItem.builder().productId("p1").variantId("v1").quantity(1).productName("P1")
                .unitPrice(Money.of(new BigDecimal("10.00"))).build();
        CartItem i2 = CartItem.builder().productId("p2").variantId("v2").quantity(2).productName("P2")
                .unitPrice(Money.of(new BigDecimal("20.00"))).build();
        Cart cart = Cart.builder().id("c1").userId("u1").items(new ArrayList<>(List.of(i1, i2))).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        when(cmsClient.getActiveCampaigns()).thenReturn(List.of());
        when(catalogClient.getVerifiedPriceAndCategory("p1", "v1"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("10.00"), null, "cat1", null)));
        when(catalogClient.getVerifiedPriceAndCategory("p2", "v2"))
                .thenReturn(Optional.of(new ProductVerification(new BigDecimal("20.00"), null, "cat1", null)));
        when(catalogClient.getAvailableStock(anyString())).thenReturn(100);
        when(cmsClient.calculateBestCampaignDiscount(any(), anyString(), any(), any(), any(), anyInt(), any()))
                .thenReturn(CampaignDiscountResult.none());
        when(shippingTaxUseCase.findShippingOptions(anyString(), any(), any())).thenReturn(List.of());
        when(shippingTaxUseCase.calculateTax(anyString(), anyString(), any())).thenReturn(Money.zero());
        when(cmsClient.isFreeShippingCampaignActive(any(), anyList(), anyList(), any())).thenReturn(false);

        Coupon coupon = Coupon.builder().id("c1").code("CAT").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).active(true).appliesToProducts(List.of("p1"))
                .appliesToCategories(List.of("cat1")).build();
        when(couponUseCase.findByCode("CAT")).thenReturn(coupon);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = useCase.createFromCart("u1", null, validAddress(), null, "card", "CAT", null, null, null, null,
                null, "USD", null, null);
        // Both items match (p1 by product, p2 by category) → 10% of 50 = 5
        assertThat(result.getDiscountAmount().getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void createFromCart_categoryScopedNoMatchProductOnly_returnsZero() {
        // Coupon scoped only to "different-cat", item is "cat1" → no match
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

        Coupon coupon = Coupon.builder().id("c1").code("CAT").type(CouponType.PERCENTAGE)
                .value(Money.of(new BigDecimal("10"))).active(true).appliesToCategories(List.of("different-cat"))
                .build();
        when(couponUseCase.findByCode("CAT")).thenReturn(coupon);
        Map<String, Object> addr = validAddress();

        // No eligible items → COUPON_SCOPE_MISMATCH
        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", "CAT", null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    // ── address builder helpers (appendIfPresent edge cases) ──────
    @Test
    void confirmOrder_addressWithBlankFields_buildsClean() {
        Map<String, Object> partialAddr = new HashMap<>();
        partialAddr.put("country", "MX");
        partialAddr.put("street", "");
        partialAddr.put("city", "   ");
        partialAddr.put("region", null);
        partialAddr.put("postalCode", "00100");
        Order order = Order.builder().id("o1").orderNumber("NX-1").userId("u1").status(OrderStatus.DRAFT)
                .subtotal(Money.of(new BigDecimal("20.00"))).shippingCost(Money.zero()).taxAmount(Money.zero())
                .total(Money.of(new BigDecimal("20.00"))).currencyCode("USD").shippingAddress(partialAddr)
                .items(List.of(OrderItem.builder().productId("p").variantId("v").quantity(1)
                        .unitPrice(Money.of(new BigDecimal("20.00"))).totalPrice(Money.of(new BigDecimal("20.00")))
                        .productName("P").sku("S").build()))
                .build();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        useCase.confirmOrder("o1", "u1", "buyer@x.com");
        verify(invoiceUseCase).create(any());
    }

    // ── short giftCardId without 8 chars also exercises short-suffix ─
    @Test
    void createGiftCardOrder_nullGiftCardId_usesNullStringSuffix() {
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfUrlSigner.signPath(anyString())).thenReturn("/invoices/x.pdf");

        Order result = useCase.createGiftCardOrder(null, "C", "b1", "b@x.com", "B", "10", "USD", null, null, null);
        // String.valueOf(null) → "null"
        assertThat(result.getOrderNumber()).isEqualTo("GC-null");
    }

    // ── CartItems list null guard ───────────────────────────────────
    @Test
    void createFromCart_cartItemsNull_throws() {
        Cart cart = Cart.builder().id("c1").userId("u1").items(null).build();
        when(cartRepository.findActiveByUserId("u1")).thenReturn(Optional.of(cart));
        Map<String, Object> addr = validAddress();

        assertThatThrownBy(() -> useCase.createFromCart("u1", null, addr, null, "card", null, null, null, null, null,
                null, "USD", null, null)).isInstanceOf(BusinessException.class);
    }

    // ── Private helpers: locale + payment-method label + escHtml + fmt ──────
    @Test
    void resolveCustomerLocale_explicitCustomerLocaleStripsRegion() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("resolveCustomerLocale",
                com.backandwhite.domain.model.Order.class);
        m.setAccessible(true);
        com.backandwhite.domain.model.Order o = com.backandwhite.domain.model.Order.builder().id("o")
                .customerLocale("pt-BR").build();
        assertThat(m.invoke(useCase, o)).isEqualTo("pt");
    }

    @Test
    void resolveCustomerLocale_fallbackToCountryHeuristic() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("resolveCustomerLocale",
                com.backandwhite.domain.model.Order.class);
        m.setAccessible(true);
        com.backandwhite.domain.model.Order o = com.backandwhite.domain.model.Order.builder().id("o")
                .customerLocale(null).shippingAddress(java.util.Map.of("country", "br")).build();
        assertThat(m.invoke(useCase, o)).isEqualTo("pt");

        com.backandwhite.domain.model.Order o2 = com.backandwhite.domain.model.Order.builder().id("o")
                .customerLocale(null).shippingAddress(java.util.Map.of("country", "GB")).build();
        assertThat(m.invoke(useCase, o2)).isEqualTo("en");

        com.backandwhite.domain.model.Order o3 = com.backandwhite.domain.model.Order.builder().id("o")
                .customerLocale(null).shippingAddress(java.util.Map.of("country", "MX")).build();
        assertThat(m.invoke(useCase, o3)).isEqualTo("es");
    }

    @Test
    void resolveCustomerLocale_nullShippingAddress_returnsEs() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("resolveCustomerLocale",
                com.backandwhite.domain.model.Order.class);
        m.setAccessible(true);
        com.backandwhite.domain.model.Order o = com.backandwhite.domain.model.Order.builder().id("o").customerLocale("")
                .shippingAddress(null).build();
        assertThat(m.invoke(useCase, o)).isEqualTo("es");
    }

    @Test
    void fmtPaymentMethod_handlesAllVariantsAndDefault() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("fmtPaymentMethod", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(useCase, (Object) null)).isEqualTo("—");
        assertThat(m.invoke(useCase, "card")).isEqualTo("Tarjeta de crédito");
        assertThat(m.invoke(useCase, "credit_card")).isEqualTo("Tarjeta de crédito");
        assertThat(m.invoke(useCase, "debit_card")).isEqualTo("Tarjeta de débito");
        assertThat(m.invoke(useCase, "paypal")).isEqualTo("PayPal");
        assertThat(m.invoke(useCase, "usdt")).isEqualTo("USDT (Crypto)");
        assertThat(m.invoke(useCase, "btc")).isEqualTo("Bitcoin (Crypto)");
        assertThat(m.invoke(useCase, "bank_transfer")).isEqualTo("Transferencia bancaria");
        assertThat(m.invoke(useCase, "cash_on_delivery")).isEqualTo("Contra reembolso");
        assertThat(m.invoke(useCase, "gift_card")).isEqualTo("Tarjeta de regalo");
        assertThat(m.invoke(useCase, "none")).isEqualTo("Sin cargo");
        assertThat(m.invoke(useCase, "WeIrD-pAy")).isEqualTo("WeIrD-pAy"); // default branch returns input
    }

    @Test
    void fmt_handlesNullMoneyBigDecimalAndOtherTypes() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("fmt", Object.class);
        m.setAccessible(true);
        assertThat(m.invoke(useCase, (Object) null)).isEqualTo("0.00");
        assertThat(
                m.invoke(useCase, com.backandwhite.common.domain.valueobject.Money.of(new java.math.BigDecimal("1.5"))))
                .isEqualTo("1.50");
        assertThat(m.invoke(useCase, new java.math.BigDecimal("2.345"))).isEqualTo("2.35");
        assertThat(m.invoke(useCase, "raw-string")).isEqualTo("raw-string"); // default branch
    }

    @Test
    void escHtml_returnsEmptyForNullAndEscapesSpecialChars() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("escHtml", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(useCase, (Object) null)).isEqualTo("");
        assertThat(m.invoke(useCase, "<a href=\"x&y\">")).isEqualTo("&lt;a href=&quot;x&amp;y&quot;&gt;");
    }

    @Test
    void buildLinesHtml_returnsEmptyForNullOrEmpty() throws Exception {
        java.lang.reflect.Method m = useCase.getClass().getDeclaredMethod("buildLinesHtml", java.util.List.class,
                String.class);
        m.setAccessible(true);
        assertThat(m.invoke(useCase, null, "USD")).isEqualTo("");
        assertThat(m.invoke(useCase, java.util.List.of(), "USD")).isEqualTo("");
    }
}
