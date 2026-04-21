package com.backandwhite.infrastructure.client.cj;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.domain.model.CjFreightOption;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.model.CjTrackInfo;
import com.backandwhite.domain.model.Order;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Local / testing adapter that replaces the real {@link CjShoppingClient} when
 * {@code app.cj.mock-mode=true}. Every method returns a deterministic synthetic
 * response so the full local flow runs without touching CJ's HTTP API: order
 * creation succeeds, the saga pipeline flips each step to DONE, balance and
 * tracking read plausible numbers.
 *
 * <p>
 * Activated via Spring property — the real {@link CjShoppingClient} is still
 * registered as a bean but this one is {@link Primary} and wins injection when
 * the flag is on. Setting the flag back to {@code false} and restarting the
 * service reverts to live CJ.
 * </p>
 */
@Log4j2
@Component
@Primary
@ConditionalOnProperty(name = "app.cj.mock-mode", havingValue = "true")
public class MockCjShoppingAdapter implements CjShoppingPort {

    private static final String MOCK_PREFIX = "MOCK_CJ_";

    public MockCjShoppingAdapter() {
        log.warn("╔═══════════════════════════════════════════════════════════════╗");
        log.warn("║  CJ MOCK MODE ACTIVE — no HTTP calls will be made to CJ.    ║");
        log.warn("║  All responses are synthesised locally. Flip                ║");
        log.warn("║  app.cj.mock-mode=false to talk to the real provider.       ║");
        log.warn("╚═══════════════════════════════════════════════════════════════╝");
    }

    @Override
    public CjOrder createOrder(Order order) {
        String cjOrderId = MOCK_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("::> [MOCK] createOrder orderId={} → cjOrderId={}", order.getId(), cjOrderId);
        return CjOrder.builder().orderId(order.getId()).cjOrderId(cjOrderId).cjOrderStatus(CjOrderStatus.UNPAID)
                .build();
    }

    @Override
    public CjOrder getOrderDetail(String cjOrderId) {
        log.info("::> [MOCK] getOrderDetail cjOrderId={}", cjOrderId);
        return CjOrder.builder().cjOrderId(cjOrderId).cjOrderStatus(CjOrderStatus.UNSHIPPED)
                .trackNumber("MOCK_TRK_" + cjOrderId).build();
    }

    @Override
    public String getBalance() {
        return "9999.00";
    }

    @Override
    public BigDecimal getBalanceAmount() {
        return new BigDecimal("9999.00");
    }

    @Override
    public CjFulfillmentResult addCart(String cjOrderId) {
        log.info("::> [MOCK] addCart cjOrderId={}", cjOrderId);
        return CjFulfillmentResult.builder().success(true).build();
    }

    @Override
    public CjFulfillmentResult addCartConfirm(String cjOrderId) {
        log.info("::> [MOCK] addCartConfirm cjOrderId={}", cjOrderId);
        return CjFulfillmentResult.builder().success(true)
                .shipmentsId("MOCK_SHIP_" + UUID.randomUUID().toString().substring(0, 8)).build();
    }

    @Override
    public CjFulfillmentResult generateParentOrder(String shipmentOrderId) {
        log.info("::> [MOCK] generateParentOrder shipmentOrderId={}", shipmentOrderId);
        return CjFulfillmentResult.builder().success(true)
                .payId("MOCK_PAY_" + UUID.randomUUID().toString().substring(0, 8))
                .actualPayment(new BigDecimal("12.50")).postage(new BigDecimal("5.00"))
                .productAmount(new BigDecimal("7.50")).taxFee(BigDecimal.ZERO).build();
    }

    @Override
    public void payBalanceV2(String shipmentOrderId, String payId) {
        log.info("::> [MOCK] payBalanceV2 shipmentOrderId={} payId={} (no real charge)", shipmentOrderId, payId);
    }

    @Override
    public List<CjFreightOption> calculateFreight(String toCountryCode, List<CjFreightOption.ProductItem> products) {
        log.info("::> [MOCK] calculateFreight toCountry={} items={}", toCountryCode,
                products != null ? products.size() : 0);
        return List.of(
                CjFreightOption.builder().logisticName("MOCK CJPacket Ordinary").logisticPrice(new BigDecimal("5.00"))
                        .logisticAging("12-18").build(),
                CjFreightOption.builder().logisticName("MOCK CJPacket Express").logisticPrice(new BigDecimal("12.50"))
                        .logisticAging("5-9").build());
    }

    @Override
    public CjTrackInfo getTrackInfo(String trackNumber) {
        log.info("::> [MOCK] getTrackInfo trackNumber={}", trackNumber);
        return CjTrackInfo.builder().trackingNumber(trackNumber).trackingStatus("IN_TRANSIT")
                .lastMileCarrier("MOCK Carrier").deliveryDay("7").build();
    }

    @Override
    public void deleteOrder(String cjOrderId) {
        log.info("::> [MOCK] deleteOrder cjOrderId={} (no-op)", cjOrderId);
    }

    @Override
    public String getRegisteredWebhookUrl() {
        return "https://mock.nx036.local/api/v1/cj/webhook/mock-secret/order";
    }

    @Override
    public void registerWebhookUrl(String callbackUrl) {
        log.info("::> [MOCK] registerWebhookUrl callbackUrl={} (no-op)", callbackUrl);
    }
}
