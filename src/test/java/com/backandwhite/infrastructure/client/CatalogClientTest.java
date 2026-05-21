package com.backandwhite.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CatalogPort.ProductVerification;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("CatalogClient")
class CatalogClientTest {

    private CatalogClient client;
    private RestClient restClient;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() throws Exception {
        client = new CatalogClient("http://localhost:6002");
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        Field f = CatalogClient.class.getDeclaredField("restClient");
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

    // ─── getVerifiedPriceAndCategory ──────────────────────────────────────

    @Test
    @DisplayName("getVerifiedPriceAndCategory returns empty when body is null")
    void verifiedPrice_nullBody_returnsEmpty() {
        stubBody(null);
        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getVerifiedPriceAndCategory uses variant retailPrice and variantSellPrice")
    void verifiedPrice_variantWithRetailAndCost() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("vid", "v1");
        variant.put("retailPrice", "20.00");
        variant.put("variantSellPrice", "12.00");
        variant.put("variantWeight", "0.5");
        Map<String, Object> body = new HashMap<>();
        body.put("categoryId", "cat-1");
        body.put("variants", List.of(variant));
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "v1");

        assertThat(result).isPresent();
        ProductVerification v = result.get();
        assertThat(v.price()).isEqualByComparingTo("20.00");
        assertThat(v.costPrice()).isEqualByComparingTo("12.00");
        assertThat(v.categoryId()).isEqualTo("cat-1");
        assertThat(v.weight()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("variant retailPrice without cost falls back to retailPrice as cost")
    void verifiedPrice_variantRetailNoCost() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("vid", "v1");
        variant.put("retailPrice", "9.99");
        Map<String, Object> body = new HashMap<>();
        body.put("variants", List.of(variant));
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "v1");

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("9.99");
        assertThat(result.get().costPrice()).isEqualByComparingTo("9.99");
        assertThat(result.get().weight()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("variant with only variantSellPrice (no retailPrice) uses cost as both")
    void verifiedPrice_variantOnlyCost() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("vid", "v1");
        variant.put("variantSellPrice", "5.50");
        Map<String, Object> body = new HashMap<>();
        body.put("variants", List.of(variant));
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "v1");

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("5.50");
        assertThat(result.get().costPrice()).isEqualByComparingTo("5.50");
    }

    @Test
    @DisplayName("variantId not found falls through to product-level prices")
    void verifiedPrice_variantNotFound_fallsBack() {
        Map<String, Object> body = new HashMap<>();
        body.put("variants", List.of(Map.of("vid", "other")));
        body.put("sellPriceRaw", "15.00");
        body.put("costPriceRaw", "10.00");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "missing");

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("15.00");
        assertThat(result.get().costPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("blank variantId path: uses product-level sellPriceRaw")
    void verifiedPrice_blankVariantId_productLevel() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPriceRaw", "8.00");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "");

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("8.00");
        assertThat(result.get().costPrice()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("product-level sellPriceRaw without costPriceRaw uses retail as cost")
    void verifiedPrice_productLevel_noCost() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPriceRaw", "12.34");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", null);

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("12.34");
        assertThat(result.get().costPrice()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("falls back to parsing range-style sellPrice string")
    void verifiedPrice_sellPriceRange() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPrice", "1.17 -- 1.22");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", null);

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("1.17");
    }

    @Test
    @DisplayName("blank sellPrice string returns empty")
    void verifiedPrice_blankSellPrice_empty() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPrice", "  ");
        stubBody(body);

        assertThat(client.getVerifiedPriceAndCategory("p1", null)).isEmpty();
    }

    @Test
    @DisplayName("non-numeric sellPrice string returns empty")
    void verifiedPrice_nonNumericSellPrice_empty() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPrice", "free");
        stubBody(body);

        assertThat(client.getVerifiedPriceAndCategory("p1", null)).isEmpty();
    }

    @Test
    @DisplayName("body with no price fields at all returns empty")
    void verifiedPrice_noPrices_empty() {
        stubBody(new HashMap<>());
        assertThat(client.getVerifiedPriceAndCategory("p1", null)).isEmpty();
    }

    @Test
    @DisplayName("RestClient exception is swallowed and Optional.empty returned")
    void verifiedPrice_runtimeException_returnsEmpty() {
        stubBodyThrows(new ResourceAccessException("network"));
        assertThat(client.getVerifiedPriceAndCategory("p1", "v1")).isEmpty();
    }

    @Test
    @DisplayName("null categoryId in body is preserved")
    void verifiedPrice_nullCategory() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPriceRaw", "5.00");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", null);

        assertThat(result).isPresent();
        assertThat(result.get().categoryId()).isNull();
    }

    @Test
    @DisplayName("variants list is null on the body (variant path skipped)")
    void verifiedPrice_nullVariants_fallsBack() {
        Map<String, Object> body = new HashMap<>();
        body.put("sellPriceRaw", "7.00");
        stubBody(body);

        Optional<ProductVerification> result = client.getVerifiedPriceAndCategory("p1", "v1");

        assertThat(result).isPresent();
        assertThat(result.get().price()).isEqualByComparingTo("7.00");
    }

    // ─── getAvailableStock ────────────────────────────────────────────────

    @Test
    @DisplayName("getAvailableStock returns the available count from response")
    void stock_returnsAvailable() {
        Map<String, Object> body = new HashMap<>();
        body.put("available", 42);
        stubBody(body);

        assertThat(client.getAvailableStock("v1")).isEqualTo(42);
    }

    @Test
    @DisplayName("getAvailableStock returns -1 when result is null")
    void stock_nullBody_returnsMinusOne() {
        stubBody(null);
        assertThat(client.getAvailableStock("v1")).isEqualTo(-1);
    }

    @Test
    @DisplayName("getAvailableStock returns -1 when 'available' key missing")
    void stock_missingKey_returnsMinusOne() {
        stubBody(new HashMap<>());
        assertThat(client.getAvailableStock("v1")).isEqualTo(-1);
    }

    @Test
    @DisplayName("getAvailableStock returns -1 on RuntimeException")
    void stock_exception_returnsMinusOne() {
        stubBodyThrows(new ResourceAccessException("timeout"));
        assertThat(client.getAvailableStock("v1")).isEqualTo(-1);
    }
}
