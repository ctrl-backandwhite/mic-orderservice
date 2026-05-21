package com.backandwhite.infrastructure.client.cj.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backandwhite.domain.exception.ExternalServiceException;
import com.backandwhite.infrastructure.client.cj.dto.CjAccessTokenDataDto;
import com.backandwhite.infrastructure.client.cj.dto.CjApiResponseDto;
import com.backandwhite.infrastructure.configuration.CjDropshippingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@DisplayName("CjShoppingAuthClient")
class CjShoppingAuthClientTest {

    private ExchangeFunction exchangeFunction;
    private WebClient webClient;
    private CjDropshippingProperties properties;
    private CjShoppingAuthClient client;

    @BeforeEach
    void setUp() {
        exchangeFunction = mock(ExchangeFunction.class);
        webClient = WebClient.builder().baseUrl("http://cj.test").exchangeFunction(exchangeFunction).build();
        properties = new CjDropshippingProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("http://cj.test");
        client = new CjShoppingAuthClient(webClient, properties);
    }

    private ClientResponse jsonResponse(String body) {
        return ClientResponse.create(HttpStatus.OK).header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(body)
                .build();
    }

    private CjApiResponseDto<CjAccessTokenDataDto> tokenSuccess() {
        CjApiResponseDto<CjAccessTokenDataDto> r = new CjApiResponseDto<>();
        r.setCode("200");
        r.setResult(true);
        r.setMessage("ok");
        CjAccessTokenDataDto d = new CjAccessTokenDataDto();
        d.setAccessToken("AT123");
        d.setRefreshToken("RT123");
        d.setAccessTokenExpiryDate("2099-01-01 00:00:00");
        d.setRefreshTokenExpiryDate("2099-12-31 00:00:00");
        r.setData(d);
        return r;
    }

    private static String jsonOk = """
            {"code":"200","result":true,"message":"ok",\
            "data":{"accessToken":"AT123","refreshToken":"RT123",\
            "accessTokenExpiryDate":"2099-01-01 00:00:00",\
            "refreshTokenExpiryDate":"2099-12-31 00:00:00"}}
            """;

    private static String jsonNullData = """
            {"code":"200","result":true,"message":"ok","data":null}
            """;

    // ─── requestNewToken ──────────────────────────────────────────────────

    @Test
    @DisplayName("requestNewToken returns parsed token on happy path")
    void requestNewToken_success() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(jsonResponse(jsonOk)));
        CjAccessTokenDataDto data = client.requestNewToken();
        assertThat(data.getAccessToken()).isEqualTo("AT123");
        assertThat(data.getRefreshToken()).isEqualTo("RT123");
    }

    @Test
    @DisplayName("requestNewToken throws when data is null")
    void requestNewToken_nullData_throws() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(jsonResponse(jsonNullData)));
        assertThatThrownBy(() -> client.requestNewToken()).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("requestNewToken WebClient error wraps to ExternalServiceException")
    void requestNewToken_webClientError() {
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.error(new WebClientRequestException(new RuntimeException("boom"), null,
                        java.net.URI.create("http://cj.test"), org.springframework.http.HttpHeaders.EMPTY)));
        assertThatThrownBy(() -> client.requestNewToken()).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("requestNewToken unexpected runtime exception wraps to ExternalServiceException")
    void requestNewToken_unexpectedError() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new IllegalStateException("kaboom")));
        assertThatThrownBy(() -> client.requestNewToken()).isInstanceOf(ExternalServiceException.class);
    }

    // ─── refreshAccessToken ───────────────────────────────────────────────

    @Test
    @DisplayName("refreshAccessToken returns parsed token on happy path")
    void refresh_success() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(jsonResponse(jsonOk)));
        CjAccessTokenDataDto data = client.refreshAccessToken("RT_OLD");
        assertThat(data.getAccessToken()).isEqualTo("AT123");
    }

    @Test
    @DisplayName("refreshAccessToken throws when data null")
    void refresh_nullData_throws() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(jsonResponse(jsonNullData)));
        assertThatThrownBy(() -> client.refreshAccessToken("RT_OLD")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("refreshAccessToken WebClient error wraps to ExternalServiceException")
    void refresh_webClientError() {
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.error(new WebClientRequestException(new RuntimeException("dns"), null,
                        java.net.URI.create("http://cj.test"), org.springframework.http.HttpHeaders.EMPTY)));
        assertThatThrownBy(() -> client.refreshAccessToken("RT_OLD")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("refreshAccessToken unexpected error wraps to ExternalServiceException")
    void refresh_unexpectedError() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new IllegalStateException("kaboom")));
        assertThatThrownBy(() -> client.refreshAccessToken("RT_OLD")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("requestNewToken: empty Mono (response null) → ExternalServiceException")
    void requestNewToken_emptyResponse() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.empty());
        assertThatThrownBy(() -> client.requestNewToken()).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("refreshAccessToken: empty Mono (response null) → ExternalServiceException")
    void refresh_emptyResponse() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.empty());
        assertThatThrownBy(() -> client.refreshAccessToken("RT_OLD")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("dummy reference for unused tokenSuccess() helper coverage")
    void unusedHelperReference() {
        // The mock test above sends raw JSON; this asserts the helper builds a valid
        // payload too.
        CjApiResponseDto<CjAccessTokenDataDto> r = tokenSuccess();
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getData().getAccessToken()).isEqualTo("AT123");
    }
}
