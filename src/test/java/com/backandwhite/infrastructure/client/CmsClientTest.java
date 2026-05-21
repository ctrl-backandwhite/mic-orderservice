package com.backandwhite.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CmsPort.CampaignDiscountResult;
import com.backandwhite.common.domain.valueobject.Money;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("CmsClient")
class CmsClientTest {

    private CmsClient client;
    private RestClient restClient;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() throws Exception {
        client = new CmsClient("http://localhost:6006");
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        Field f = CmsClient.class.getDeclaredField("restClient");
        f.setAccessible(true);
        f.set(client, restClient);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(uriSpec.uri(anyString(), any(Object.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubBody(Object body) {
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(body);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubBodyThrows(RuntimeException ex) {
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenThrow(ex);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubMapClass(Map body) {
        when(responseSpec.body(Map.class)).thenReturn(body);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubMapClassThrows(RuntimeException ex) {
        when(responseSpec.body(Map.class)).thenThrow(ex);
    }

    // ─── getActiveCampaigns ───────────────────────────────────────────────

    @Test
    @DisplayName("getActiveCampaigns returns list when present")
    void activeCampaigns_returnsList() {
        List<Map<String, Object>> camps = List.of(Map.of("id", "c1"));
        stubBody(camps);
        assertThat(client.getActiveCampaigns()).hasSize(1).containsExactlyElementsOf(camps);
    }

    @Test
    @DisplayName("getActiveCampaigns returns empty when body is null")
    void activeCampaigns_nullBody_empty() {
        stubBody(null);
        assertThat(client.getActiveCampaigns()).isEmpty();
    }

    @Test
    @DisplayName("getActiveCampaigns returns empty when call throws")
    void activeCampaigns_throws_returnsEmpty() {
        stubBodyThrows(new ResourceAccessException("network"));
        assertThat(client.getActiveCampaigns()).isEmpty();
    }

    // ─── calculateBestCampaignDiscount ────────────────────────────────────

    @Test
    @DisplayName("PERCENTAGE on margin: 50% off margin (price 20, cost 12) = 4")
    void percentageDiscount() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("id", "PC");
        camp.put("type", "PERCENTAGE");
        camp.put("value", "50");
        camp.put("appliesToProducts", List.of("p1"));

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", "cat", Money.of("20"),
                Money.of("12"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("4.00");
        assertThat(r.campaignId()).isEqualTo("PC");
    }

    @Test
    @DisplayName("FLASH treated as percentage discount on margin")
    void flashDiscount() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FLASH");
        camp.put("value", "10");
        camp.put("id", "F1");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", "cat", Money.of("100"),
                Money.of("50"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("FIXED is capped at margin")
    void fixedCappedAtMargin() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FIXED");
        camp.put("value", "20"); // bigger than margin (8)
        camp.put("id", "FX");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("20"),
                Money.of("12"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("BUY2GET1 with qty 3: per-unit discount = (1 free * margin)/3")
    void buy2Get1Qualifies() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUY2GET1");
        camp.put("id", "B");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("0"), 3, Money.of("90"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("BUY2GET1 below threshold yields zero discount")
    void buy2Get1Below() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUY2GET1");
        camp.put("id", "B");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("10"), 2, Money.of("60"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("BUNDLE buyQty=2 getQty=1 with qty=3 picks 1 free per group")
    void bundleQualifies() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUNDLE");
        camp.put("buyQty", "2");
        camp.put("getQty", "1");
        camp.put("id", "BU");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("0"), 3, Money.of("90"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("BUNDLE without buyQty/getQty yields zero")
    void bundleZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUNDLE");
        camp.put("id", "BU");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("0"), 3, Money.of("90"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("BUNDLE qty below buy+get yields zero")
    void bundleBelow() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUNDLE");
        camp.put("buyQty", "2");
        camp.put("getQty", "1");
        camp.put("id", "BU");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("0"), 2, Money.of("60"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("FREE_SHIPPING returns zero in product-discount calc (handled separately)")
    void freeShippingZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("id", "FS");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("10"), 1, Money.of("60"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Unknown type yields zero discount")
    void unknownType() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "MYSTERY");
        camp.put("id", "X");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("10"), 1, Money.of("60"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Campaign with neither product nor category lists applies to all")
    void appliesToAllWhenNoScope() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "10");
        camp.put("id", "ALL");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "any", "anyCat", Money.of("100"),
                Money.of("0"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Campaign matched by category list")
    void matchedByCategory() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "20");
        camp.put("appliesToCategories", List.of("cat-1"));
        camp.put("id", "CAT");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", "cat-1", Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("Campaign skipped because product not in scope")
    void notInProductScope() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "20");
        camp.put("appliesToProducts", List.of("other"));
        camp.put("id", "X");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", "cat", Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.campaignId()).isNull();
    }

    @Test
    @DisplayName("minOrder threshold skips campaign when subtotal too low")
    void minOrderSkips() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "50");
        camp.put("minOrder", "100");
        camp.put("id", "MIN");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("20"),
                Money.of("0"), 1, Money.of("50"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("minOrder=0 is ignored (no skip)")
    void minOrderZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "10");
        camp.put("minOrder", "0");
        camp.put("id", "Z");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("20"),
                Money.of("0"), 1, Money.of("5"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("maxDiscount caps the calculated discount")
    void maxDiscountCap() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "100");
        camp.put("maxDiscount", "5");
        camp.put("id", "M");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("100"),
                Money.of("0"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Negative margin (cost > price) yields zero discount")
    void negativeMarginGuard() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "50");
        camp.put("id", "NEG");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("5"),
                Money.of("10"), 1, Money.of("5"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Best discount among multiple campaigns is chosen")
    void picksBestAmongMany() {
        Map<String, Object> small = new HashMap<>();
        small.put("type", "PERCENTAGE");
        small.put("value", "10");
        small.put("id", "small");
        Map<String, Object> big = new HashMap<>();
        big.put("type", "PERCENTAGE");
        big.put("value", "50");
        big.put("id", "big");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(small, big), "p", null, Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.campaignId()).isEqualTo("big");
        assertThat(r.discount().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Empty/null type field defaults to zero discount")
    void nullTypeYieldsZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("value", "50");
        camp.put("id", null);

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("20"),
                Money.of("10"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── getExchangeRate ──────────────────────────────────────────────────

    @Test
    @DisplayName("getExchangeRate returns 1 for null currency")
    void exchangeRate_null() {
        assertThat(client.getExchangeRate(null)).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getExchangeRate returns 1 for blank currency")
    void exchangeRate_blank() {
        assertThat(client.getExchangeRate("  ")).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getExchangeRate returns 1 for USD")
    void exchangeRate_usd() {
        assertThat(client.getExchangeRate("usd")).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getExchangeRate parses rate from response body")
    void exchangeRate_parses() {
        Map<String, Object> body = new HashMap<>();
        body.put("rate", "0.926");
        stubMapClass(body);

        assertThat(client.getExchangeRate("EUR")).isEqualByComparingTo("0.926");
    }

    @Test
    @DisplayName("getExchangeRate returns 1 when rate field absent")
    void exchangeRate_noRate() {
        stubMapClass(new HashMap<>());
        assertThat(client.getExchangeRate("EUR")).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getExchangeRate returns 1 when body null")
    void exchangeRate_nullBody() {
        stubMapClass(null);
        assertThat(client.getExchangeRate("EUR")).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("getExchangeRate returns 1 when call throws")
    void exchangeRate_throws() {
        stubMapClassThrows(new ResourceAccessException("network"));
        assertThat(client.getExchangeRate("EUR")).isEqualByComparingTo(BigDecimal.ONE);
    }

    // ─── isFreeShippingCampaignActive ─────────────────────────────────────

    @Test
    @DisplayName("free shipping with no scope applies to all")
    void freeShipping_allScope() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p"), List.of("c"), Money.of("50"));

        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("free shipping skipped when not FREE_SHIPPING type")
    void freeShipping_wrongType() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p"), List.of("c"), Money.of("50"));

        assertThat(res).isFalse();
    }

    @Test
    @DisplayName("free shipping skipped when below minOrder")
    void freeShipping_belowMinOrder() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("minOrder", "100");

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p"), List.of("c"), Money.of("50"));

        assertThat(res).isFalse();
    }

    @Test
    @DisplayName("free shipping minOrder=0 doesn't skip")
    void freeShipping_minOrderZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("minOrder", "0");

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p"), List.of("c"), Money.of("5"));

        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("free shipping all products covered by product list")
    void freeShipping_allCoveredByProducts() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("appliesToProducts", List.of("p1", "p2"));

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p1", "p2"), List.of(),
                Money.of("50"));

        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("free shipping covered via categories")
    void freeShipping_coveredByCategories() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("appliesToCategories", List.of("c1"));

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p1"), List.of("c1"), Money.of("50"));

        assertThat(res).isTrue();
    }

    @Test
    @DisplayName("free shipping rejected when one product uncovered")
    void freeShipping_oneUncovered() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "FREE_SHIPPING");
        camp.put("appliesToProducts", List.of("p1"));

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p1", "p2"), List.of(),
                Money.of("50"));

        assertThat(res).isFalse();
    }

    @Test
    @DisplayName("free shipping rejected when no campaigns at all")
    void freeShipping_noCampaigns() {
        boolean res = client.isFreeShippingCampaignActive(List.of(), List.of("p"), List.of(), Money.of("50"));
        assertThat(res).isFalse();
    }

    @Test
    @DisplayName("calculate: campaign matched by category list — categoryId branch true")
    void matchesByCategoryWithProductFallback() {
        // appliesToProducts contains a different product, but appliesToCategories
        // matches
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "10");
        camp.put("appliesToProducts", List.of("other"));
        camp.put("appliesToCategories", List.of("cat-1"));
        camp.put("id", "X");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", "cat-1", Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("calculate: empty productIds and empty categoryIds → applies-to-all branch")
    void calculate_emptyScope() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "10");
        camp.put("appliesToProducts", List.of());
        camp.put("appliesToCategories", List.of());
        camp.put("id", "ALL");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", "cat", Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("BUNDLE buyQty=2 getQty=0 yields zero (getQty<=0 branch)")
    void bundleGetQtyZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "BUNDLE");
        camp.put("buyQty", "2");
        camp.put("getQty", "0");
        camp.put("id", "BU");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p1", null, Money.of("30"),
                Money.of("0"), 5, Money.of("150"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculate: maxDiscount=0 doesn't cap")
    void maxDiscountZero() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "50");
        camp.put("maxDiscount", "0");
        camp.put("id", "M0");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("100"),
                Money.of("0"), 1, Money.of("100"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("calculate: campaign without 'id' field → bestCampaignId is null")
    void campaignWithoutId() {
        Map<String, Object> camp = new HashMap<>();
        camp.put("type", "PERCENTAGE");
        camp.put("value", "50");

        CampaignDiscountResult r = client.calculateBestCampaignDiscount(List.of(camp), "p", null, Money.of("20"),
                Money.of("0"), 1, Money.of("20"));

        assertThat(r.discount().getAmount()).isEqualByComparingTo("10.00");
        assertThat(r.campaignId()).isNull();
    }

    @Test
    @DisplayName("free shipping campaign without 'type' field is skipped")
    void freeShipping_noType() {
        Map<String, Object> camp = new HashMap<>();
        // no 'type' key
        camp.put("appliesToProducts", List.of("p"));

        boolean res = client.isFreeShippingCampaignActive(List.of(camp), List.of("p"), List.of(), Money.of("50"));
        assertThat(res).isFalse();
    }
}
