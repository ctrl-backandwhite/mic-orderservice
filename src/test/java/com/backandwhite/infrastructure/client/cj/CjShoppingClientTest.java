package com.backandwhite.infrastructure.client.cj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backandwhite.common.domain.valueobject.Money;
import com.backandwhite.domain.exception.ExternalServiceException;
import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.client.cj.auth.CjShoppingTokenManager;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
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

@DisplayName("CjShoppingClient")
class CjShoppingClientTest {

    private ExchangeFunction exchangeFunction;
    private WebClient webClient;
    private CjShoppingTokenManager tokenManager;
    private CjShoppingClient client;

    @BeforeEach
    void setUp() throws Exception {
        exchangeFunction = mock(ExchangeFunction.class);
        webClient = WebClient.builder().baseUrl("http://cj.test").exchangeFunction(exchangeFunction).build();
        tokenManager = mock(CjShoppingTokenManager.class);
        when(tokenManager.getValidAccessToken()).thenReturn("AT_OK");

        client = new CjShoppingClient(webClient, tokenManager);

        // @Value fields aren't auto-set when constructed manually
        Field a = CjShoppingClient.class.getDeclaredField("defaultLogisticName");
        a.setAccessible(true);
        a.set(client, "CJPacket Ordinary");
        Field b = CjShoppingClient.class.getDeclaredField("fromCountryCode");
        b.setAccessible(true);
        b.set(client, "CN");
    }

    private ClientResponse jsonOk(String json) {
        return ClientResponse.create(HttpStatus.OK).header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(json)
                .build();
    }

    private void stubExchange(String json) {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(jsonOk(json)));
    }

    private void stubExchangeError(Throwable t) {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(t));
    }

    private static WebClientRequestException webClientErr() {
        return new WebClientRequestException(new RuntimeException("net"), null, java.net.URI.create("http://cj.test"),
                org.springframework.http.HttpHeaders.EMPTY);
    }

    private static Order sampleOrder() {
        return Order.builder().id("ord-1").orderNumber("ORD-1").notes("note")
                .shippingAddress(Map.of("fullName", "Jane Doe", "country", "MX", "region", "CDMX", "city", "MX-City",
                        "street", "1 main", "street2", "ap 2", "postalCode", "01000", "phone", "555", "email",
                        "x@x.com", "houseNumber", "10"))
                .items(List.of(OrderItem.builder().productId("p1").variantId("v1").quantity(2).build(),
                        OrderItem.builder().productId("p2").quantity(1).unitPrice(Money.of("1")).build()))
                .build();
    }

    // ─── createOrder ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder: success → CjOrder with cjOrderId")
    void createOrder_success() {
        stubExchange("""
                {"code":"200","result":true,"message":"ok",
                 "data":{"orderId":"CJ123","orderNum":"ORN1","createTime":"now",
                  "productList":[{"vid":"v1","quantity":2}]}}
                """);
        CjOrder result = client.createOrder(sampleOrder());
        assertThat(result.getCjOrderId()).isEqualTo("CJ123");
        assertThat(result.getOrderId()).isEqualTo("ord-1");
        assertThat(result.getCjOrderStatus()).isEqualTo(CjOrderStatus.UNPAID);
        assertThat(result.getProductInfoList()).isNotNull();
    }

    @Test
    @DisplayName("createOrder: result=false → ExternalServiceException")
    void createOrder_resultFalse_throws() {
        stubExchange("""
                {"code":"500","result":false,"message":"bad","data":null}
                """);
        Order order = sampleOrder();
        assertThatThrownBy(() -> client.createOrder(order)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("createOrder: data=null → ExternalServiceException")
    void createOrder_dataNull_throws() {
        stubExchange("""
                {"code":"200","result":true,"message":"ok","data":null}
                """);
        Order order = sampleOrder();
        assertThatThrownBy(() -> client.createOrder(order)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("createOrder: WebClient error → wraps to ExternalServiceException")
    void createOrder_webClientError() {
        stubExchangeError(webClientErr());
        Order order = sampleOrder();
        assertThatThrownBy(() -> client.createOrder(order)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("createOrder: unexpected exception → wraps to ExternalServiceException")
    void createOrder_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        Order order = sampleOrder();
        assertThatThrownBy(() -> client.createOrder(order)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("createOrder: shippingAddress null and single-word fullName")
    void createOrder_minimalAddress() {
        Order order = Order.builder().id("ord-2").orderNumber("ORD-2").notes(null).shippingAddress(null)
                .items(List.of(OrderItem.builder().productId("p1").variantId(null).quantity(1).build())).build();
        stubExchange("""
                {"code":"200","result":true,"message":"ok",
                 "data":{"orderId":"CJ7","productList":null}}
                """);
        CjOrder result = client.createOrder(order);
        assertThat(result.getCjOrderId()).isEqualTo("CJ7");
        // S1168 fix: productListToMap now returns an empty map (not null) when the
        // source list is null.
        assertThat(result.getProductInfoList()).isEmpty();
    }

    @Test
    @DisplayName("createOrder: single-word fullName: lastName empty")
    void createOrder_singleWordName() {
        Order order = Order.builder().id("ord-3").orderNumber("ORD-3").shippingAddress(Map.of("fullName", "Madonna"))
                .items(List.of(OrderItem.builder().productId("p1").quantity(1).build())).build();
        stubExchange("""
                {"code":"200","result":true,"message":"ok",
                 "data":{"orderId":"CJ8","productList":[]}}
                """);
        CjOrder result = client.createOrder(order);
        assertThat(result.getCjOrderId()).isEqualTo("CJ8");
    }

    // ─── getOrderDetail ───────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderDetail: parses status and tracking")
    void detail_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{
                 "orderId":"CJ1","orderStatus":"SHIPPED","trackNumber":"TRK","logisticName":"DHL",
                 "shipmentOrderId":"SHIP1","productList":[]}}
                """);
        CjOrder result = client.getOrderDetail("CJ1");
        assertThat(result.getCjOrderId()).isEqualTo("CJ1");
        assertThat(result.getTrackNumber()).isEqualTo("TRK");
        assertThat(result.getLogisticName()).isEqualTo("DHL");
        assertThat(result.getShipmentOrderId()).isEqualTo("SHIP1");
    }

    @Test
    @DisplayName("getOrderDetail: data=null → ExternalServiceException")
    void detail_nullData_throws() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThatThrownBy(() -> client.getOrderDetail("CJ1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("getOrderDetail: WebClient error → ExternalServiceException")
    void detail_webClientError() {
        stubExchangeError(webClientErr());
        assertThatThrownBy(() -> client.getOrderDetail("CJ1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("getOrderDetail: unexpected error → ExternalServiceException")
    void detail_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        assertThatThrownBy(() -> client.getOrderDetail("CJ1")).isInstanceOf(ExternalServiceException.class);
    }

    // ─── getBalance / getBalanceAmount ────────────────────────────────────

    @Test
    @DisplayName("getBalance: returns balance + currency")
    void balance_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{"balance":"100.50","currency":"USD"}}
                """);
        assertThat(client.getBalance()).isEqualTo("100.50 USD");
    }

    @Test
    @DisplayName("getBalance: data null → unavailable")
    void balance_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.getBalance()).isEqualTo("unavailable");
    }

    @Test
    @DisplayName("getBalance: error → unavailable")
    void balance_error() {
        stubExchangeError(new RuntimeException("boom"));
        assertThat(client.getBalance()).isEqualTo("unavailable");
    }

    @Test
    @DisplayName("getBalanceAmount: returns numeric value")
    void balanceAmount_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{"balance":"500.00","currency":"USD"}}
                """);
        assertThat(client.getBalanceAmount()).isEqualByComparingTo("500.00");
    }

    // S5976 suppressed: each case uses a different stub method (stubExchange vs
    // stubExchangeError)
    // and a different JSON payload, so a clean parameterization isn't possible
    // without losing
    // the readability of the per-case @DisplayName.
    @SuppressWarnings("java:S5976")
    @Test
    @DisplayName("getBalanceAmount: data null → null")
    void balanceAmount_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.getBalanceAmount()).isNull();
    }

    @Test
    @DisplayName("getBalanceAmount: blank balance → null")
    void balanceAmount_blank() {
        stubExchange("""
                {"code":"200","result":true,"data":{"balance":"","currency":"USD"}}
                """);
        assertThat(client.getBalanceAmount()).isNull();
    }

    @Test
    @DisplayName("getBalanceAmount: error → null")
    void balanceAmount_error() {
        stubExchangeError(new RuntimeException("net"));
        assertThat(client.getBalanceAmount()).isNull();
    }

    // ─── addCart ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addCart: success → success=true")
    void addCart_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{"successCount":1,"interceptOrders":[]}}
                """);
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("addCart: result=false → success=false with message")
    void addCart_resultFalse() {
        stubExchange("""
                {"code":"500","result":false,"message":"forbidden","data":null}
                """);
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorReason()).contains("forbidden");
    }

    @Test
    @DisplayName("addCart: intercepted order → success=false with reason joined")
    void addCart_intercepted() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"successCount":0,"interceptOrders":[{"cjOrderId":"CJ1","reason":"NO_STOCK"}]}}
                """);
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorReason()).contains("NO_STOCK");
    }

    @Test
    @DisplayName("addCart: WebClient error → success=false")
    void addCart_webClientError() {
        stubExchangeError(webClientErr());
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCart: unexpected error → success=false")
    void addCart_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    // ─── addCartConfirm ───────────────────────────────────────────────────

    @Test
    @DisplayName("addCartConfirm: success with shipmentsId")
    void confirm_success() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"submitSuccess":true,"shipmentsId":"SHIP1","interceptOrders":[]}}
                """);
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getShipmentsId()).isEqualTo("SHIP1");
    }

    @Test
    @DisplayName("addCartConfirm: data null → success=false")
    void confirm_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCartConfirm: result=false → success=false")
    void confirm_resultFalse() {
        stubExchange("""
                {"code":"500","result":false,"message":"err","data":null}
                """);
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCartConfirm: submitSuccess=false with intercepts")
    void confirm_submitFalse_intercepts() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"submitSuccess":false,"shipmentsId":null,
                  "interceptOrders":[{"cjOrderId":"CJ1","reason":"NO_ROUTE"}]}}
                """);
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorReason()).contains("NO_ROUTE");
    }

    @Test
    @DisplayName("addCartConfirm: submitSuccess=false without intercepts uses fallback message")
    void confirm_submitFalse_noIntercepts() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"submitSuccess":false,"shipmentsId":null,"interceptOrders":null}}
                """);
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorReason()).contains("submitSuccess=false");
    }

    @Test
    @DisplayName("addCartConfirm: WebClient error → success=false")
    void confirm_webClientError() {
        stubExchangeError(webClientErr());
        assertThat(client.addCartConfirm("CJ1").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCartConfirm: unexpected error → success=false")
    void confirm_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        assertThat(client.addCartConfirm("CJ1").isSuccess()).isFalse();
    }

    // ─── generateParentOrder ──────────────────────────────────────────────

    @Test
    @DisplayName("generateParentOrder: success with payment info")
    void parent_success() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"payId":"PAY1",
                  "paymentInformation":{"actualPayment":12.5,"postage":5.0,"productAmount":7.0,"taxFee":0.5}}}
                """);
        CjFulfillmentResult r = client.generateParentOrder("SHIP1");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getPayId()).isEqualTo("PAY1");
        assertThat(r.getActualPayment()).isEqualByComparingTo("12.5");
        assertThat(r.getPostage()).isEqualByComparingTo("5.0");
        assertThat(r.getProductAmount()).isEqualByComparingTo("7.0");
        assertThat(r.getTaxFee()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("generateParentOrder: success but paymentInformation null leaves amounts null")
    void parent_nullPaymentInfo() {
        stubExchange("""
                {"code":"200","result":true,
                 "data":{"payId":"PAY1","paymentInformation":null}}
                """);
        CjFulfillmentResult r = client.generateParentOrder("SHIP1");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getPayId()).isEqualTo("PAY1");
        assertThat(r.getActualPayment()).isNull();
    }

    // S5976 suppressed: cases differ in stub method (stubExchange vs
    // stubExchangeError) and the
    // type of error injected, which prevents a clean single-input parameterization.
    @SuppressWarnings("java:S5976")
    @Test
    @DisplayName("generateParentOrder: data null → success=false")
    void parent_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.generateParentOrder("SHIP1").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("generateParentOrder: WebClient error → success=false")
    void parent_webClientError() {
        stubExchangeError(webClientErr());
        assertThat(client.generateParentOrder("SHIP1").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("generateParentOrder: unexpected error → success=false")
    void parent_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        assertThat(client.generateParentOrder("SHIP1").isSuccess()).isFalse();
    }

    // ─── payBalanceV2 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("payBalanceV2: success returns normally")
    void pay_success() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThatCode(() -> client.payBalanceV2("SHIP1", "PAY1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("payBalanceV2: result=false → ExternalServiceException")
    void pay_resultFalse() {
        stubExchange("""
                {"code":"500","result":false,"message":"declined","data":null}
                """);
        assertThatThrownBy(() -> client.payBalanceV2("SHIP1", "PAY1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("payBalanceV2: WebClient error → ExternalServiceException")
    void pay_webClientError() {
        stubExchangeError(webClientErr());
        assertThatThrownBy(() -> client.payBalanceV2("SHIP1", "PAY1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("payBalanceV2: unexpected error → ExternalServiceException")
    void pay_unexpectedError() {
        stubExchangeError(new IllegalStateException("kaboom"));
        assertThatThrownBy(() -> client.payBalanceV2("SHIP1", "PAY1")).isInstanceOf(ExternalServiceException.class);
    }

    // ─── calculateFreight ─────────────────────────────────────────────────

    @Test
    @DisplayName("calculateFreight: success returns mapped options")
    void freight_success() {
        stubExchange("""
                {"code":"200","result":true,"data":[
                 {"logisticName":"CJPacket","logisticPrice":12.5,"logisticPriceCn":85.0,
                  "logisticAging":"7-14","taxesFee":0,"clearanceOperationFee":0}]}
                """);
        List<CjFreightOption> result = client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLogisticName()).isEqualTo("CJPacket");
        assertThat(result.get(0).getLogisticPrice()).isEqualByComparingTo("12.5");
    }

    @Test
    @DisplayName("calculateFreight: result false → empty list")
    void freight_resultFalse_empty() {
        stubExchange("""
                {"code":"500","result":false,"message":"err","data":null}
                """);
        assertThat(client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()))).isEmpty();
    }

    @Test
    @DisplayName("calculateFreight: data=null → empty list")
    void freight_nullData_empty() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()))).isEmpty();
    }

    @Test
    @DisplayName("calculateFreight: WebClient error → empty list")
    void freight_webClientError_empty() {
        stubExchangeError(webClientErr());
        assertThat(client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()))).isEmpty();
    }

    @Test
    @DisplayName("calculateFreight: unexpected error → empty list")
    void freight_unexpectedError_empty() {
        stubExchangeError(new IllegalStateException("kaboom"));
        assertThat(client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()))).isEmpty();
    }

    // ─── getTrackInfo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getTrackInfo: returns mapped info")
    void track_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{
                 "trackingNumber":"TRK1","logisticName":"DHL","trackingFrom":"CN","trackingTo":"MX",
                 "deliveryDay":"7","deliveryTime":"12:00","trackingStatus":"IN_TRANSIT",
                 "lastMileCarrier":"FedEx","lastTrackNumber":"LMT1"}}
                """);
        CjTrackInfo info = client.getTrackInfo("TRK1");
        assertThat(info.getTrackingNumber()).isEqualTo("TRK1");
        assertThat(info.getTrackingStatus()).isEqualTo("IN_TRANSIT");
    }

    @Test
    @DisplayName("getTrackInfo: data null → null")
    void track_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.getTrackInfo("TRK1")).isNull();
    }

    @Test
    @DisplayName("getTrackInfo: error → null")
    void track_error_returnsNull() {
        stubExchangeError(new RuntimeException("net"));
        assertThat(client.getTrackInfo("TRK1")).isNull();
    }

    // ─── deleteOrder ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOrder: success → no throw")
    void delete_success() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThatCode(() -> client.deleteOrder("CJ1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteOrder: result=false → no throw (warn only)")
    void delete_resultFalse() {
        stubExchange("""
                {"code":"500","result":false,"message":"nope","data":null}
                """);
        assertThatCode(() -> client.deleteOrder("CJ1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteOrder: error → no throw")
    void delete_error() {
        stubExchangeError(new RuntimeException("net"));
        assertThatCode(() -> client.deleteOrder("CJ1")).doesNotThrowAnyException();
    }

    // ─── getRegisteredWebhookUrl ──────────────────────────────────────────

    @Test
    @DisplayName("getRegisteredWebhookUrl: parses callBackUrl")
    void webhookGet_success() {
        stubExchange("""
                {"code":"200","result":true,"data":{"callBackUrl":"https://x/webhook"}}
                """);
        assertThat(client.getRegisteredWebhookUrl()).isEqualTo("https://x/webhook");
    }

    @Test
    @DisplayName("getRegisteredWebhookUrl: data null → empty string")
    void webhookGet_nullData() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThat(client.getRegisteredWebhookUrl()).isEmpty();
    }

    @Test
    @DisplayName("getRegisteredWebhookUrl: missing callBackUrl key → empty string")
    void webhookGet_missingKey() {
        stubExchange("""
                {"code":"200","result":true,"data":{"other":"x"}}
                """);
        assertThat(client.getRegisteredWebhookUrl()).isEmpty();
    }

    @Test
    @DisplayName("getRegisteredWebhookUrl: error → empty string")
    void webhookGet_error() {
        stubExchangeError(new RuntimeException("net"));
        assertThat(client.getRegisteredWebhookUrl()).isEmpty();
    }

    // ─── registerWebhookUrl ───────────────────────────────────────────────

    @Test
    @DisplayName("registerWebhookUrl: success returns normally")
    void webhookSet_success() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        assertThatCode(() -> client.registerWebhookUrl("https://x/webhook")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registerWebhookUrl: code 1606000 (already registered) is treated as success")
    void webhookSet_alreadyExists() {
        stubExchange("""
                {"code":"1606000","result":false,"message":"exists","data":null}
                """);
        assertThatCode(() -> client.registerWebhookUrl("https://x/webhook")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registerWebhookUrl: response null → IllegalStateException")
    void webhookSet_nullResponse_throws() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.empty());
        assertThatThrownBy(() -> client.registerWebhookUrl("https://x/webhook"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("registerWebhookUrl: error code → IllegalStateException")
    void webhookSet_errorCode_throws() {
        stubExchange("""
                {"code":"500","result":false,"message":"server-failure","data":null}
                """);
        assertThatThrownBy(() -> client.registerWebhookUrl("https://x/webhook"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("registerWebhookUrl: WebClient error → IllegalStateException")
    void webhookSet_webClientError() {
        stubExchangeError(webClientErr());
        assertThatThrownBy(() -> client.registerWebhookUrl("https://x/webhook"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── empty (null) response branches ───────────────────────────────────

    private void stubEmpty() {
        when(exchangeFunction.exchange(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("createOrder: empty response → throws (response==null branch)")
    void createOrder_emptyResponse() {
        stubEmpty();
        Order order = sampleOrder();
        assertThatThrownBy(() -> client.createOrder(order)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("getOrderDetail: empty response → throws")
    void detail_emptyResponse() {
        stubEmpty();
        assertThatThrownBy(() -> client.getOrderDetail("CJ1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("getBalance: empty response → unavailable")
    void balance_emptyResponse() {
        stubEmpty();
        assertThat(client.getBalance()).isEqualTo("unavailable");
    }

    @Test
    @DisplayName("getBalanceAmount: empty response → null")
    void balanceAmount_emptyResponse() {
        stubEmpty();
        assertThat(client.getBalanceAmount()).isNull();
    }

    @Test
    @DisplayName("addCart: empty response → success=false")
    void addCart_emptyResponse() {
        stubEmpty();
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCart: data null leaves hasIntercepts=false (success path with null data)")
    void addCart_nullDataSuccess() {
        stubExchange("""
                {"code":"200","result":true,"data":null}
                """);
        CjFulfillmentResult r = client.addCart("CJ1");
        assertThat(r.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("addCart: empty intercepts list (size==0) → success=true")
    void addCart_emptyIntercepts() {
        stubExchange("""
                {"code":"200","result":true,"data":{"successCount":1,"interceptOrders":[]}}
                """);
        assertThat(client.addCart("CJ1").isSuccess()).isTrue();
    }

    @Test
    @DisplayName("addCart: data not null but interceptOrders null → success=true (hasIntercepts=false)")
    void addCart_dataNotNullInterceptsNull() {
        stubExchange("""
                {"code":"200","result":true,"data":{"successCount":1,"interceptOrders":null}}
                """);
        assertThat(client.addCart("CJ1").isSuccess()).isTrue();
    }

    @Test
    @DisplayName("generateParentOrder: result=false → success=false")
    void parent_resultFalse() {
        stubExchange("""
                {"code":"500","result":false,"message":"err","data":null}
                """);
        assertThat(client.generateParentOrder("SHIP1").isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addCartConfirm: empty response → success=false")
    void confirm_emptyResponse() {
        stubEmpty();
        CjFulfillmentResult r = client.addCartConfirm("CJ1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("generateParentOrder: empty response → success=false")
    void parent_emptyResponse() {
        stubEmpty();
        CjFulfillmentResult r = client.generateParentOrder("SHIP1");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("payBalanceV2: empty response → ExternalServiceException")
    void pay_emptyResponse() {
        stubEmpty();
        assertThatThrownBy(() -> client.payBalanceV2("SHIP1", "PAY1")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("calculateFreight: empty response → empty list (logs 'null')")
    void freight_emptyResponse() {
        stubEmpty();
        assertThat(client.calculateFreight("MX",
                List.of(CjFreightOption.ProductItem.builder().vid("v1").quantity(1).build()))).isEmpty();
    }

    @Test
    @DisplayName("getTrackInfo: empty response → null")
    void track_emptyResponse() {
        stubEmpty();
        assertThat(client.getTrackInfo("TRK1")).isNull();
    }

    @Test
    @DisplayName("deleteOrder: empty response → no throw with 'null response'")
    void delete_emptyResponse() {
        stubEmpty();
        assertThatCode(() -> client.deleteOrder("CJ1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getRegisteredWebhookUrl: empty response → empty string")
    void webhookGet_emptyResponse() {
        stubEmpty();
        assertThat(client.getRegisteredWebhookUrl()).isEmpty();
    }

    @Test
    @DisplayName("getBalanceAmount: balance null (raw==null branch) → null")
    void balanceAmount_nullBalance() {
        stubExchange("""
                {"code":"200","result":true,"data":{"balance":null,"currency":"USD"}}
                """);
        assertThat(client.getBalanceAmount()).isNull();
    }

}
