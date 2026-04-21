package com.backandwhite.infrastructure.client.cj;

import static com.backandwhite.domain.exception.Message.*;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.domain.exception.ExternalServiceException;
import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.model.OrderItem;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import com.backandwhite.infrastructure.client.cj.auth.CjShoppingTokenManager;
import com.backandwhite.infrastructure.client.cj.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Log4j2
@Component
@RequiredArgsConstructor
public class CjShoppingClient implements CjShoppingPort {

    private static final Duration DATA_TIMEOUT = Duration.ofSeconds(30);
    private static final String RESILIENCE4J_INSTANCE = "cjShopping";

    @Qualifier("cjShoppingWebClient")
    private final WebClient cjShoppingWebClient;
    private final CjShoppingTokenManager cjShoppingTokenManager;

    @Value("${app.cj.default-logistic-name:CJPacket Ordinary}")
    private String defaultLogisticName;

    @Value("${app.cj.from-country-code:CN}")
    private String fromCountryCode;

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjOrder createOrder(Order order) {
        log.info("::> Submitting order={} to CJ Shopping API...", order.getId());
        String accessToken = cjShoppingTokenManager.getValidAccessToken();

        CjCreateOrderV3RequestDto request = buildCreateOrderRequest(order);

        try {
            CjApiResponseDto<CjCreateOrderV3ResponseDto> response = cjShoppingWebClient.post()
                    .uri("/shopping/order/createOrderV3").header("CJ-Access-Token", accessToken).bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjCreateOrderV3ResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();

            if (response == null || !response.isSuccess() || response.getData() == null) {
                String msg = response != null ? response.getMessage() : "null response";
                throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(order.getId(), msg);
            }

            CjCreateOrderV3ResponseDto data = response.getData();
            log.info("::> Order={} submitted to CJ. cjOrderId={}", order.getId(), data.getOrderId());

            return CjOrder.builder().orderId(order.getId()).cjOrderId(data.getOrderId())
                    .cjOrderStatus(CjOrderStatus.UNPAID).productInfoList(productListToMap(data.getProductList()))
                    .build();

        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientException e) {
            log.error("::> CJ create order failed for orderId={} (WebClient): {}", order.getId(), e.getMessage(), e);
            throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(order.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("::> CJ create order failed for orderId={} (unexpected): {}", order.getId(), e.getMessage(), e);
            throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(order.getId(), e.getMessage());
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjOrder getOrderDetail(String cjOrderId) {
        log.info("::> Fetching CJ order detail for cjOrderId={}...", cjOrderId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();

        try {
            CjApiResponseDto<CjOrderDetailResponseDto> response = cjShoppingWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/shopping/order/getOrderDetailByOrderId")
                            .queryParam("orderId", cjOrderId).build())
                    .header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjOrderDetailResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();

            if (response == null || response.getData() == null) {
                throw CJ_DATA_ERROR.toExternalServiceException("CJ order detail cjOrderId=" + cjOrderId);
            }

            CjOrderDetailResponseDto data = response.getData();
            return CjOrder.builder().cjOrderId(data.getOrderId()).shipmentOrderId(data.getShipmentOrderId())
                    .cjOrderStatus(CjOrderStatus.fromString(data.getOrderStatus())).trackNumber(data.getTrackNumber())
                    .logisticName(data.getLogisticName()).productInfoList(productListToMap(data.getProductList()))
                    .build();

        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientException e) {
            log.error("::> CJ get order detail failed for cjOrderId={} (WebClient): {}", cjOrderId, e.getMessage(), e);
            throw CJ_DATA_ERROR.toExternalServiceException("CJ order detail: " + e.getMessage());
        } catch (Exception e) {
            log.error("::> CJ get order detail failed for cjOrderId={} (unexpected): {}", cjOrderId, e.getMessage(), e);
            throw CJ_DATA_ERROR.toExternalServiceException("CJ order detail: " + e.getMessage());
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public String getBalance() {
        log.info("::> Querying CJ Shopping account balance...");
        String accessToken = cjShoppingTokenManager.getValidAccessToken();

        try {
            CjApiResponseDto<CjBalanceResponseDto> response = cjShoppingWebClient.get()
                    .uri("/shopping/account/getAccountBalance").header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjBalanceResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();

            if (response == null || response.getData() == null) {
                return "unavailable";
            }
            CjBalanceResponseDto data = response.getData();
            return data.getBalance() + " " + data.getCurrency();

        } catch (Exception e) {
            log.warn("::> CJ balance query failed: {}", e.getMessage());
            return "unavailable";
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public BigDecimal getBalanceAmount() {
        log.debug("::> Querying CJ account balance (numeric)...");
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjApiResponseDto<CjBalanceResponseDto> response = cjShoppingWebClient.get()
                    .uri("/shopping/account/getAccountBalance").header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjBalanceResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || response.getData() == null)
                return null;
            String raw = response.getData().getBalance();
            if (raw == null || raw.isBlank())
                return null;
            return new BigDecimal(raw);
        } catch (Exception e) {
            log.warn("::> CJ balance numeric query failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjFulfillmentResult addCart(String cjOrderId) {
        log.info("::> CJ addCart cjOrderId={}", cjOrderId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjAddCartRequestDto req = CjAddCartRequestDto.builder().cjOrderIdList(List.of(cjOrderId)).build();
            CjApiResponseDto<CjAddCartResponseDto> response = cjShoppingWebClient.post().uri("/shopping/order/addCart")
                    .header("CJ-Access-Token", accessToken).bodyValue(req).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjAddCartResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess()) {
                String msg = response != null ? response.getMessage() : "null response";
                log.warn("::> CJ addCart failed for {}: {}", cjOrderId, msg);
                return CjFulfillmentResult.builder().success(false).errorReason(msg).build();
            }
            CjAddCartResponseDto data = response.getData();
            boolean hasIntercepts = data != null && data.getInterceptOrders() != null
                    && !data.getInterceptOrders().isEmpty();
            if (hasIntercepts) {
                String reason = data.getInterceptOrders().stream().map(i -> i.getCjOrderId() + ": " + i.getReason())
                        .collect(Collectors.joining("; "));
                log.warn("::> CJ addCart intercepted {}: {}", cjOrderId, reason);
                return CjFulfillmentResult.builder().success(false).errorReason(reason).build();
            }
            return CjFulfillmentResult.builder().success(true).build();
        } catch (WebClientException e) {
            log.error("::> CJ addCart WebClient error for {}: {}", cjOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        } catch (Exception e) {
            log.error("::> CJ addCart unexpected error for {}: {}", cjOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjFulfillmentResult addCartConfirm(String cjOrderId) {
        log.info("::> CJ addCartConfirm for cjOrderId={}", cjOrderId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjAddCartRequestDto req = CjAddCartRequestDto.builder().cjOrderIdList(List.of(cjOrderId)).build();
            CjApiResponseDto<CjAddCartConfirmResponseDto> response = cjShoppingWebClient.post()
                    .uri("/shopping/order/addCartConfirm").header("CJ-Access-Token", accessToken).bodyValue(req)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjAddCartConfirmResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess() || response.getData() == null) {
                String msg = response != null ? response.getMessage() : "null response";
                log.warn("::> CJ addCartConfirm failed for {}: {}", cjOrderId, msg);
                return CjFulfillmentResult.builder().success(false).errorReason(msg).build();
            }
            CjAddCartConfirmResponseDto data = response.getData();
            if (Boolean.FALSE.equals(data.getSubmitSuccess())) {
                String reason = data.getInterceptOrders() != null
                        ? data.getInterceptOrders().stream().map(i -> i.getCjOrderId() + ": " + i.getReason())
                                .collect(Collectors.joining("; "))
                        : "submitSuccess=false";
                return CjFulfillmentResult.builder().success(false).errorReason(reason).build();
            }
            return CjFulfillmentResult.builder().success(true).shipmentsId(data.getShipmentsId()).build();
        } catch (WebClientException e) {
            log.error("::> CJ addCartConfirm WebClient error for {}: {}", cjOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        } catch (Exception e) {
            log.error("::> CJ addCartConfirm unexpected error for {}: {}", cjOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjFulfillmentResult generateParentOrder(String shipmentOrderId) {
        log.info("::> CJ generateParentOrder shipmentsId={}", shipmentOrderId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjGenerateParentOrderRequestDto req = CjGenerateParentOrderRequestDto.builder()
                    .shipmentOrderId(shipmentOrderId).build();
            CjApiResponseDto<CjGenerateParentOrderResponseDto> response = cjShoppingWebClient.post()
                    .uri("/shopping/order/saveGenerateParentOrder").header("CJ-Access-Token", accessToken)
                    .bodyValue(req).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjGenerateParentOrderResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess() || response.getData() == null) {
                String msg = response != null ? response.getMessage() : "null response";
                log.warn("::> CJ generateParentOrder failed for {}: {}", shipmentOrderId, msg);
                return CjFulfillmentResult.builder().success(false).errorReason(msg).build();
            }
            CjGenerateParentOrderResponseDto data = response.getData();
            BigDecimal actualPayment = null, postage = null, productAmount = null, taxFee = null;
            if (data.getPaymentInformation() != null) {
                actualPayment = data.getPaymentInformation().getActualPayment();
                postage = data.getPaymentInformation().getPostage();
                productAmount = data.getPaymentInformation().getProductAmount();
                taxFee = data.getPaymentInformation().getTaxFee();
            }
            return CjFulfillmentResult.builder().success(true).payId(data.getPayId()).actualPayment(actualPayment)
                    .postage(postage).productAmount(productAmount).taxFee(taxFee).build();
        } catch (WebClientException e) {
            log.error("::> CJ generateParentOrder WebClient error for {}: {}", shipmentOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        } catch (Exception e) {
            log.error("::> CJ generateParentOrder unexpected error for {}: {}", shipmentOrderId, e.getMessage(), e);
            return CjFulfillmentResult.builder().success(false).errorReason(e.getMessage()).build();
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public void payBalanceV2(String shipmentOrderId, String payId) {
        log.info("::> CJ payBalanceV2 shipmentsId={} payId={}", shipmentOrderId, payId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjPayBalanceV2RequestDto req = CjPayBalanceV2RequestDto.builder().shipmentOrderId(shipmentOrderId)
                    .payId(payId).build();
            CjApiResponseDto<Object> response = cjShoppingWebClient.post().uri("/shopping/pay/payBalanceV2")
                    .header("CJ-Access-Token", accessToken).bodyValue(req).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<Object>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess()) {
                String msg = response != null ? response.getMessage() : "null response";
                throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(shipmentOrderId, "payBalanceV2: " + msg);
            }
            log.info("::> CJ payBalanceV2 succeeded for shipmentsId={}", shipmentOrderId);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientException e) {
            log.error("::> CJ payBalanceV2 WebClient error for {}: {}", shipmentOrderId, e.getMessage(), e);
            throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(shipmentOrderId, "payBalanceV2: " + e.getMessage());
        } catch (Exception e) {
            log.error("::> CJ payBalanceV2 unexpected error for {}: {}", shipmentOrderId, e.getMessage(), e);
            throw CJ_ORDER_SUBMIT_FAILED.toExternalServiceException(shipmentOrderId, "payBalanceV2: " + e.getMessage());
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public List<CjFreightOption> calculateFreight(String toCountryCode, List<CjFreightOption.ProductItem> products) {
        log.info("::> CJ calculateFreight to={} products={}", toCountryCode, products.size());
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            List<CjFreightCalculateRequestDto.ProductItem> dtoProducts = products.stream()
                    .map(p -> CjFreightCalculateRequestDto.ProductItem.builder().vid(p.getVid())
                            .quantity(p.getQuantity()).build())
                    .collect(Collectors.toList());
            CjFreightCalculateRequestDto req = CjFreightCalculateRequestDto.builder().startCountryCode(fromCountryCode)
                    .endCountryCode(toCountryCode).products(dtoProducts).build();
            CjApiResponseDto<List<CjFreightCalculateResponseDto>> response = cjShoppingWebClient.post()
                    .uri("/logistic/freightCalculate").header("CJ-Access-Token", accessToken).bodyValue(req).retrieve()
                    .bodyToMono(
                            new ParameterizedTypeReference<CjApiResponseDto<List<CjFreightCalculateResponseDto>>>() {
                            })
                    .timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("::> CJ calculateFreight returned empty/failed: {}",
                        response != null ? response.getMessage() : "null");
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .map(d -> CjFreightOption.builder().logisticName(d.getLogisticName())
                            .logisticPrice(d.getLogisticPrice()).logisticPriceCn(d.getLogisticPriceCn())
                            .logisticAging(d.getLogisticAging()).taxesFee(d.getTaxesFee())
                            .clearanceOperationFee(d.getClearanceOperationFee()).build())
                    .collect(Collectors.toList());
        } catch (WebClientException e) {
            log.error("::> CJ calculateFreight WebClient error: {}", e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("::> CJ calculateFreight unexpected error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public CjTrackInfo getTrackInfo(String trackNumber) {
        log.info("::> CJ getTrackInfo trackNumber={}", trackNumber);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjApiResponseDto<CjTrackInfoResponseDto> response = cjShoppingWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/logistic/trackInfo").queryParam("trackNumber", trackNumber)
                            .build())
                    .header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjTrackInfoResponseDto>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || response.getData() == null)
                return null;
            CjTrackInfoResponseDto d = response.getData();
            return CjTrackInfo.builder().trackingNumber(d.getTrackingNumber()).logisticName(d.getLogisticName())
                    .trackingFrom(d.getTrackingFrom()).trackingTo(d.getTrackingTo()).deliveryDay(d.getDeliveryDay())
                    .deliveryTime(d.getDeliveryTime()).trackingStatus(d.getTrackingStatus())
                    .lastMileCarrier(d.getLastMileCarrier()).lastTrackNumber(d.getLastTrackNumber()).build();
        } catch (Exception e) {
            log.warn("::> CJ getTrackInfo failed for {}: {}", trackNumber, e.getMessage());
            return null;
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public void deleteOrder(String cjOrderId) {
        log.info("::> CJ deleteOrder cjOrderId={}", cjOrderId);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();
        try {
            CjApiResponseDto<Object> response = cjShoppingWebClient.delete()
                    .uri(uriBuilder -> uriBuilder.path("/shopping/order/deleteOrder").queryParam("orderId", cjOrderId)
                            .build())
                    .header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<Object>>() {
                    }).timeout(DATA_TIMEOUT).block();
            if (response == null || !response.isSuccess()) {
                String msg = response != null ? response.getMessage() : "null response";
                log.warn("::> CJ deleteOrder failed for {}: {}", cjOrderId, msg);
            } else {
                log.info("::> CJ deleteOrder succeeded for cjOrderId={}", cjOrderId);
            }
        } catch (Exception e) {
            log.warn("::> CJ deleteOrder error for {}: {}", cjOrderId, e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private CjCreateOrderV3RequestDto buildCreateOrderRequest(Order order) {
        Map<String, Object> addr = order.getShippingAddress() != null ? order.getShippingAddress() : Map.of();

        String fullName = str(addr.get("fullName"));
        String firstName = fullName.contains(" ") ? fullName.split(" ")[0] : fullName;
        String lastName = fullName.contains(" ") ? fullName.substring(fullName.indexOf(' ') + 1) : "";

        CjCreateOrderV3RequestDto.ShippingAddressDto shippingAddress = CjCreateOrderV3RequestDto.ShippingAddressDto
                .builder().countryCode(str(addr.get("country"))).province(str(addr.get("region")))
                .city(str(addr.get("city"))).address(str(addr.get("street"))).address2(str(addr.get("street2")))
                .zipCode(str(addr.get("postalCode"))).phone(str(addr.get("phone"))).firstName(firstName)
                .lastName(lastName).email(str(addr.get("email"))).houseNumber(str(addr.get("houseNumber"))).build();

        List<CjOrderProductItemDto> products = order
                .getItems().stream().map(item -> CjOrderProductItemDto.builder().vid(getCjVariantId(item))
                        .quantity(item.getQuantity()).shippingName(defaultLogisticName).build())
                .collect(Collectors.toList());

        return CjCreateOrderV3RequestDto.builder().orderNumber(order.getOrderNumber()).shippingAddress(shippingAddress)
                .products(products).logisticName(defaultLogisticName).fromCountryCode(fromCountryCode)
                .remark(order.getNotes()).build();
    }

    private String getCjVariantId(OrderItem item) {
        // CJ variant ID is stored in variantId (mapped from CJ product sync)
        return item.getVariantId() != null ? item.getVariantId() : item.getProductId();
    }

    private Map<String, Object> productListToMap(List<CjProductInfoListItemDto> list) {
        if (list == null)
            return null;
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            result.put("item_" + i, list.get(i));
        }
        return result;
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }

    // ── Webhook registration ────────────────────────────────────────────────

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    @SuppressWarnings("unchecked")
    public String getRegisteredWebhookUrl() {
        log.info("::> Fetching registered webhook URL from CJ...");
        String accessToken = cjShoppingTokenManager.getValidAccessToken();

        try {
            CjApiResponseDto<Map<String, Object>> response = cjShoppingWebClient.get().uri("/webhook/get")
                    .header("CJ-Access-Token", accessToken).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<Map<String, Object>>>() {
                    }).timeout(DATA_TIMEOUT).block();

            if (response == null || response.getData() == null) {
                return "";
            }
            Object url = response.getData().get("callBackUrl");
            return url != null ? url.toString() : "";
        } catch (Exception e) {
            log.warn("::> CJ webhook/get failed: {}", e.getMessage());
            return "";
        }
    }

    @Override
    @Retry(name = RESILIENCE4J_INSTANCE)
    @CircuitBreaker(name = RESILIENCE4J_INSTANCE)
    public void registerWebhookUrl(String callbackUrl) {
        log.info("::> Registering CJ webhook URL: {}", callbackUrl);
        String accessToken = cjShoppingTokenManager.getValidAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("callBackUrl", callbackUrl);
        body.put("type", "ENABLE");
        body.put("messageTypeList", List.of("PRODUCT", "STOCK", "ORDER", "LOGISTICS"));

        try {
            CjApiResponseDto<Object> response = cjShoppingWebClient.post().uri("/webhook/set")
                    .header("CJ-Access-Token", accessToken).bodyValue(body).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<Object>>() {
                    }).timeout(DATA_TIMEOUT).block();

            if (response == null) {
                throw new IllegalStateException("null response from CJ /webhook/set");
            }
            // 1606000 = webhook already exists → treat as success (plan Fase 2)
            if (response.isSuccess() || "1606000".equals(response.getCode())) {
                log.info("::> CJ webhook URL registered (code={})", response.getCode());
                return;
            }
            throw new IllegalStateException(
                    "CJ /webhook/set returned code=" + response.getCode() + " msg=" + response.getMessage());
        } catch (WebClientException e) {
            log.error("::> CJ /webhook/set network error: {}", e.getMessage());
            throw new IllegalStateException("CJ webhook registration failed: " + e.getMessage(), e);
        }
    }
}
