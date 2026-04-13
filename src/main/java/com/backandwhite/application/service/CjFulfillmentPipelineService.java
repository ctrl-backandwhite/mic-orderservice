package com.backandwhite.application.service;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.application.port.out.OrderEventPort;
import com.backandwhite.domain.model.CjFulfillmentResult;
import com.backandwhite.domain.model.CjOrder;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.domain.valueobject.CjOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orchestrates the 4-step CJ fulfillment pipeline:
 * <ol>
 * <li>addCart — adds the CJ order to the shopping cart</li>
 * <li>addCartConfirm — confirms the cart and obtains shipmentsId</li>
 * <li>generateParentOrder — creates the parent order and obtains payId +
 * payment info</li>
 * <li>payBalanceV2 — pays the CJ balance</li>
 * </ol>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjFulfillmentPipelineService {

    private static final String STEP_ADD_CART = "ADD_CART";
    private static final String STEP_CART_CONFIRM = "CART_CONFIRM";
    private static final String STEP_GEN_ORDER = "GEN_ORDER";
    private static final String STEP_PAY = "PAY";
    private static final String STEP_DONE = "DONE";

    private final CjShoppingPort cjShoppingPort;
    private final CjOrderRepository cjOrderRepository;
    private final OrderEventPort orderEventPort;

    @Value("${app.cj.min-balance-alert:50.00}")
    private BigDecimal minBalanceAlert;

    /**
     * Runs the full pipeline for a given CJ order.
     * Failures are recorded to the DB (fulfillmentStep + fulfillmentError)
     * but never propagated upward — the scheduler will retry.
     */
    public void processFulfillment(CjOrder cjOrder) {
        String orderId = cjOrder.getOrderId();
        String cjOrderId = cjOrder.getCjOrderId();
        log.info("::> [Pipeline] START orderId={} cjOrderId={}", orderId, cjOrderId);

        // ── Step 1: addCart ────────────────────────────────────────────────
        CjFulfillmentResult cartResult = cjShoppingPort.addCart(cjOrderId);
        if (!cartResult.isSuccess()) {
            recordPipelineFailure(cjOrder, STEP_ADD_CART, cartResult.getErrorReason());
            publishFulfillmentFailed(orderId, STEP_ADD_CART, cartResult.getErrorReason());
            return;
        }
        log.info("::> [Pipeline] addCart OK for cjOrderId={}", cjOrderId);

        // ── Step 2: addCartConfirm ─────────────────────────────────────────
        CjFulfillmentResult confirmResult = cjShoppingPort.addCartConfirm(cjOrderId);
        if (!confirmResult.isSuccess()) {
            recordPipelineFailure(cjOrder, STEP_CART_CONFIRM, confirmResult.getErrorReason());
            publishFulfillmentFailed(orderId, STEP_CART_CONFIRM, confirmResult.getErrorReason());
            return;
        }
        String shipmentsId = confirmResult.getShipmentsId();
        log.info("::> [Pipeline] addCartConfirm OK shipmentsId={}", shipmentsId);

        // Persist shipmentsId early
        cjOrder.setShipmentsId(shipmentsId);
        cjOrder.setFulfillmentStep(STEP_GEN_ORDER);
        cjOrderRepository.save(cjOrder);

        // ── Step 3: generateParentOrder ────────────────────────────────────
        CjFulfillmentResult genResult = cjShoppingPort.generateParentOrder(shipmentsId);
        if (!genResult.isSuccess()) {
            recordPipelineFailure(cjOrder, STEP_GEN_ORDER, genResult.getErrorReason());
            publishFulfillmentFailed(orderId, STEP_GEN_ORDER, genResult.getErrorReason());
            return;
        }
        String payId = genResult.getPayId();
        BigDecimal actualPayment = genResult.getActualPayment();
        log.info("::> [Pipeline] generateParentOrder OK payId={} amount={}", payId, actualPayment);

        // Persist payId + payment amounts early
        cjOrder.setPayId(payId);
        cjOrder.setCjActualPayment(actualPayment);
        cjOrder.setCjPostageAmount(genResult.getPostage());
        cjOrder.setCjProductAmount(genResult.getProductAmount());
        cjOrder.setFulfillmentStep(STEP_PAY);
        cjOrderRepository.save(cjOrder);

        // ── Balance check ──────────────────────────────────────────────────
        BigDecimal balance = cjShoppingPort.getBalanceAmount();
        if (balance != null && actualPayment != null && balance.compareTo(actualPayment) < 0) {
            log.warn("::> [Pipeline] Insufficient CJ balance ({}) for payment ({}). orderId={}",
                    balance, actualPayment, orderId);
            cjOrder.setCjOrderStatus(CjOrderStatus.AWAITING_FUNDS);
            cjOrder.setFulfillmentStep(STEP_PAY);
            cjOrder.setFulfillmentError("Insufficient balance: " + balance + " < " + actualPayment);
            cjOrderRepository.save(cjOrder);
            publishAwaitingFunds(orderId, actualPayment, balance);
            return;
        }

        // ── Step 4: payBalanceV2 ───────────────────────────────────────────
        try {
            cjShoppingPort.payBalanceV2(shipmentsId, payId);
        } catch (Exception e) {
            recordPipelineFailure(cjOrder, STEP_PAY, e.getMessage());
            publishFulfillmentFailed(orderId, STEP_PAY, e.getMessage());
            return;
        }

        // ── Pipeline complete ──────────────────────────────────────────────
        cjOrder.setCjOrderStatus(CjOrderStatus.UNPAID); // CJ will now process
        cjOrder.setFulfillmentStep(STEP_DONE);
        cjOrder.setFulfillmentError(null);
        cjOrderRepository.save(cjOrder);
        log.info("::> [Pipeline] COMPLETE orderId={} cjOrderId={}", orderId, cjOrderId);
        publishCjOrderPaid(orderId, actualPayment);
    }

    /**
     * Resumes the pipeline from the last recorded step.
     * Called by the retry scheduler for PIPELINE_FAILED / AWAITING_FUNDS orders.
     */
    public void resumeFulfillment(CjOrder cjOrder) {
        String step = cjOrder.getFulfillmentStep();
        log.info("::> [Pipeline] RESUME orderId={} from step={}", cjOrder.getOrderId(), step);

        // Clear the failure state before retrying
        cjOrder.setCjOrderStatus(CjOrderStatus.UNPAID);
        cjOrder.setFulfillmentError(null);

        if (step == null || STEP_ADD_CART.equals(step)) {
            processFulfillment(cjOrder);
        } else if (STEP_CART_CONFIRM.equals(step)) {
            // Re-run confirm without re-adding to cart
            resumeFromConfirm(cjOrder);
        } else if (STEP_GEN_ORDER.equals(step) && cjOrder.getShipmentsId() != null) {
            resumeFromGenOrder(cjOrder);
        } else if (STEP_PAY.equals(step) && cjOrder.getShipmentsId() != null && cjOrder.getPayId() != null) {
            resumeFromPay(cjOrder);
        } else {
            // Unknown step — restart from scratch
            processFulfillment(cjOrder);
        }
    }

    // ── Private resume helpers ────────────────────────────────────────────────

    private void resumeFromConfirm(CjOrder cjOrder) {
        String cjOrderId = cjOrder.getCjOrderId();
        CjFulfillmentResult confirmResult = cjShoppingPort.addCartConfirm(cjOrderId);
        if (!confirmResult.isSuccess()) {
            recordPipelineFailure(cjOrder, STEP_CART_CONFIRM, confirmResult.getErrorReason());
            return;
        }
        cjOrder.setShipmentsId(confirmResult.getShipmentsId());
        cjOrderRepository.save(cjOrder);
        resumeFromGenOrder(cjOrder);
    }

    private void resumeFromGenOrder(CjOrder cjOrder) {
        CjFulfillmentResult genResult = cjShoppingPort.generateParentOrder(cjOrder.getShipmentsId());
        if (!genResult.isSuccess()) {
            recordPipelineFailure(cjOrder, STEP_GEN_ORDER, genResult.getErrorReason());
            return;
        }
        cjOrder.setPayId(genResult.getPayId());
        cjOrder.setCjActualPayment(genResult.getActualPayment());
        cjOrder.setCjPostageAmount(genResult.getPostage());
        cjOrder.setCjProductAmount(genResult.getProductAmount());
        cjOrderRepository.save(cjOrder);
        resumeFromPay(cjOrder);
    }

    private void resumeFromPay(CjOrder cjOrder) {
        BigDecimal balance = cjShoppingPort.getBalanceAmount();
        BigDecimal needed = cjOrder.getCjActualPayment();
        if (balance != null && needed != null && balance.compareTo(needed) < 0) {
            cjOrder.setCjOrderStatus(CjOrderStatus.AWAITING_FUNDS);
            cjOrder.setFulfillmentError("Insufficient balance: " + balance + " < " + needed);
            cjOrderRepository.save(cjOrder);
            publishAwaitingFunds(cjOrder.getOrderId(), needed, balance);
            return;
        }
        try {
            cjShoppingPort.payBalanceV2(cjOrder.getShipmentsId(), cjOrder.getPayId());
            cjOrder.setCjOrderStatus(CjOrderStatus.UNPAID);
            cjOrder.setFulfillmentStep(STEP_DONE);
            cjOrder.setFulfillmentError(null);
            cjOrderRepository.save(cjOrder);
            publishCjOrderPaid(cjOrder.getOrderId(), needed);
        } catch (Exception e) {
            recordPipelineFailure(cjOrder, STEP_PAY, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recordPipelineFailure(CjOrder cjOrder, String step, String reason) {
        log.warn("::> [Pipeline] FAILED orderId={} step={}: {}", cjOrder.getOrderId(), step, reason);
        cjOrder.setCjOrderStatus(CjOrderStatus.PIPELINE_FAILED);
        cjOrder.setFulfillmentStep(step);
        cjOrder.setFulfillmentError(reason);
        cjOrder.setErrorCount(cjOrder.getErrorCount() + 1);
        cjOrder.setUpdatedBy("PIPELINE");
        cjOrderRepository.save(cjOrder);
    }

    private void publishCjOrderPaid(String orderId, BigDecimal amount) {
        try {
            orderEventPort.publishCjOrderPaid(orderId, amount != null ? amount.toPlainString() : "0");
        } catch (Exception e) {
            log.warn("::> Could not publish cj.order.paid event for orderId={}: {}", orderId, e.getMessage());
        }
    }

    private void publishAwaitingFunds(String orderId, BigDecimal needed, BigDecimal available) {
        try {
            orderEventPort.publishCjOrderAwaitingFunds(orderId,
                    needed != null ? needed.toPlainString() : "0",
                    available != null ? available.toPlainString() : "0");
        } catch (Exception e) {
            log.warn("::> Could not publish cj.order.awaiting_funds event for orderId={}: {}", orderId, e.getMessage());
        }
    }

    private void publishFulfillmentFailed(String orderId, String step, String reason) {
        try {
            orderEventPort.publishCjFulfillmentFailed(orderId, step, reason);
        } catch (Exception e) {
            log.warn("::> Could not publish cj.fulfillment.failed event for orderId={}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Updates the CjOrder's lastWebhookAt field when a webhook is received.
     */
    public void markWebhookReceived(CjOrder cjOrder) {
        cjOrder.setLastWebhookAt(Instant.now());
        cjOrderRepository.save(cjOrder);
    }
}
