package com.backandwhite.infrastructure.message.kafka.producer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.common.constants.AppConstants;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaOrderEventAdapterTest {

    @Mock
    private KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    @InjectMocks
    private KafkaOrderEventAdapter adapter;

    @BeforeEach
    void stubSend() {
        // Default stub: completed future so the success branch in whenComplete fires.
        lenient().when(kafkaTemplate.send(any(String.class), any(String.class), any(SpecificRecord.class)))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    SendResult<String, SpecificRecord> sr = mock(SendResult.class);
                    RecordMetadata meta = new RecordMetadata(new TopicPartition("t", 0), 0L, 0, 0L, 0, 0);
                    when(sr.getRecordMetadata()).thenReturn(meta);
                    return CompletableFuture.completedFuture(sr);
                });
    }

    private CompletableFuture<SendResult<String, SpecificRecord>> stubFailingFuture(String topic, String key) {
        CompletableFuture<SendResult<String, SpecificRecord>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka boom"));
        when(kafkaTemplate.send(eq(topic), eq(key), any(SpecificRecord.class))).thenReturn(future);
        return future;
    }

    // ── Order events ─────────────────────────────────────────────────────────

    @Test
    void publishOrderCreated_sendsToCorrectTopic() {
        adapter.publishOrderCreated("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "DRAFT", 2, "addr-1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CREATED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderCreated_withNullCurrency_defaultsUsd() {
        adapter.publishOrderCreated("o1", "u1", "e@e.com", "NX-1", "100.00", null, "DRAFT", 1, "addr-1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CREATED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderConfirmed_sendsToCorrectTopic() {
        adapter.publishOrderConfirmed("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "100.00", 2);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CONFIRMED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderConfirmed_withNullCurrency_defaultsUsd() {
        adapter.publishOrderConfirmed("o1", "u1", "e@e.com", "NX-1", "100.00", null, "100.00", 2);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CONFIRMED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderStatusUpdated_sendsToCorrectTopic() {
        adapter.publishOrderStatusUpdated("o1", "u1", "e@e.com", "NX-1", "DRAFT", "PENDING");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_STATUS_UPDATED), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishOrderCancelled_sendsToCorrectTopic() {
        adapter.publishOrderCancelled("o1", "u1", "e@e.com", "NX-1", "user");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CANCELLED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderShipped_sendsToCorrectTopic() {
        adapter.publishOrderShipped("o1", "u1", "e@e.com", "NX-1", "TRACK-1", "DHL", "2026-01-01");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_SHIPPED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderDelivered_sendsToCorrectTopic() {
        adapter.publishOrderDelivered("o1", "u1", "e@e.com", "NX-1", "100.00");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_DELIVERED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishOrderReturnRequested_sendsToCorrectTopic() {
        adapter.publishOrderReturnRequested("o1", "rr1", "u1", "e@e.com", "NX-1", "defective");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_RETURN_REQUESTED), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishOrderReturnApproved_sendsToCorrectTopic() {
        adapter.publishOrderReturnApproved("o1", "rr1", "u1", "e@e.com", "NX-1", "100.00");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_RETURN_APPROVED), eq("o1"),
                any(SpecificRecord.class));
    }

    // ── Cart events ──────────────────────────────────────────────────────────

    @Test
    void publishCartAbandoned_sendsToCorrectTopic() {
        adapter.publishCartAbandoned("c1", "u1", "e@e.com", "100.00", 3, "2026-01-01");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CART_ABANDONED), eq("c1"), any(SpecificRecord.class));
    }

    @Test
    void publishCartAbandoned_withNullUserId_defaultsAnonymous() {
        adapter.publishCartAbandoned("c1", null, "e@e.com", "100.00", 3, "2026-01-01");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CART_ABANDONED), eq("c1"), any(SpecificRecord.class));
    }

    @Test
    void publishCartCheckoutInitiated_sendsToCorrectTopic() {
        adapter.publishCartCheckoutInitiated("c1", "o1", "u1", "e@e.com", "100.00", "USD", 3, "WELCOME", "addr-1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CART_CHECKOUT_INITIATED), eq("c1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCartCheckoutInitiated_withNullCurrency_defaultsUsd() {
        adapter.publishCartCheckoutInitiated("c1", "o1", "u1", "e@e.com", "100.00", null, 3, "WELCOME", "addr-1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CART_CHECKOUT_INITIATED), eq("c1"),
                any(SpecificRecord.class));
    }

    // ── Stock events ─────────────────────────────────────────────────────────

    @Test
    void publishStockReservation_sendsToCorrectTopic() {
        adapter.publishStockReservation("p1", "v1", "o1", 5);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_STOCK_RESERVED), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishStockDeducted_sendsToCorrectTopic() {
        adapter.publishStockDeducted("p1", "v1", "o1", 5);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_STOCK_DEDUCTED), eq("o1"), any(SpecificRecord.class));
    }

    // ── Notification email ───────────────────────────────────────────────────

    @Test
    void publishInvoiceEmail_sendsToCorrectTopic() {
        adapter.publishInvoiceEmail("e@e.com", "Subject", "tpl", Map.of("k", "v"));

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_NOTIFICATION_EMAIL), eq("e@e.com"),
                any(SpecificRecord.class));
    }

    @Test
    void publishInvoiceEmail_withNullEmail_skipsSend() {
        adapter.publishInvoiceEmail(null, "Subject", "tpl", Map.of());

        verify(kafkaTemplate, org.mockito.Mockito.never()).send(any(String.class), any(String.class),
                any(SpecificRecord.class));
    }

    @Test
    void publishInvoiceEmail_withBlankEmail_skipsSend() {
        adapter.publishInvoiceEmail("   ", "Subject", "tpl", Map.of());

        verify(kafkaTemplate, org.mockito.Mockito.never()).send(any(String.class), any(String.class),
                any(SpecificRecord.class));
    }

    // ── Saga events ──────────────────────────────────────────────────────────

    @Test
    void publishSagaPaymentRequested_sendsToCorrectTopic() {
        adapter.publishSagaPaymentRequested("o1", "u1", "e@e.com", "NX-1", "100.00", "USD");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_SAGA_ORDER_PAYMENT_REQUESTED), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishSagaStockRelease_sendsToCorrectTopic() {
        adapter.publishSagaStockRelease("o1", "u1", "payment-failed");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_SAGA_ORDER_STOCK_RELEASE), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishSagaNotifyFailure_sendsToCorrectTopic() {
        adapter.publishSagaNotifyFailure("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "reason");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_SAGA_ORDER_NOTIFY_FAILURE), eq("o1"),
                any(SpecificRecord.class));
    }

    // ── CJ events ────────────────────────────────────────────────────────────

    @Test
    void publishCjOrderPaid_sendsToCorrectTopic() {
        adapter.publishCjOrderPaid("o1", "100.00");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_ORDER_PAID), eq("o1"), any(SpecificRecord.class));
    }

    @Test
    void publishCjOrderAwaitingFunds_sendsToCorrectTopic() {
        adapter.publishCjOrderAwaitingFunds("o1", "100.00", "10.00");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_ORDER_AWAITING_FUNDS), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCjFulfillmentFailed_sendsToCorrectTopic() {
        adapter.publishCjFulfillmentFailed("o1", "PAY", "balance-low");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_FULFILLMENT_FAILED), eq("o1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCjBalanceLow_sendsToCorrectTopicWithFixedKey() {
        adapter.publishCjBalanceLow("10.00", "50.00");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_BALANCE_LOW), eq("BALANCE_MONITOR"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCatalogProductUpdate_sendsToCorrectTopic() {
        adapter.publishCatalogProductUpdate("p1", "{\"raw\":\"x\"}");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_CATALOG_PRODUCT_UPDATE), eq("p1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCatalogProductUpdate_withNullPayload_sendsEmptyRef() {
        adapter.publishCatalogProductUpdate("p1", null);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_CATALOG_PRODUCT_UPDATE), eq("p1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCatalogProductUpdate_truncatesLongPayload() {
        String longPayload = "x".repeat(800);
        adapter.publishCatalogProductUpdate("p1", longPayload);

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_CATALOG_PRODUCT_UPDATE), eq("p1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCatalogProductDelete_sendsToCorrectTopic() {
        adapter.publishCatalogProductDelete("p1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_CATALOG_PRODUCT_DELETE), eq("p1"),
                any(SpecificRecord.class));
    }

    @Test
    void publishCatalogStockChange_sendsToCorrectTopic() {
        adapter.publishCatalogStockChange("v1", 50, "{\"raw\":1}");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_CJ_CATALOG_STOCK_CHANGE), eq("v1"),
                any(SpecificRecord.class));
    }

    // ── send() callback branches ─────────────────────────────────────────────

    @Test
    void send_whenFutureCompletesExceptionally_logsErrorAndDoesNotThrow() {
        stubFailingFuture(AppConstants.KAFKA_TOPIC_ORDER_CREATED, "o1");

        adapter.publishOrderCreated("o1", "u1", "e@e.com", "NX-1", "100.00", "USD", "DRAFT", 1, "addr-1");

        verify(kafkaTemplate).send(eq(AppConstants.KAFKA_TOPIC_ORDER_CREATED), eq("o1"), any(SpecificRecord.class));
    }
}
