package com.backandwhite.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.api.dto.webhook.CjLogisticWebhookParams;
import com.backandwhite.api.dto.webhook.CjOrderSplitWebhookParams;
import com.backandwhite.api.dto.webhook.CjOrderWebhookParams;
import com.backandwhite.api.dto.webhook.CjWebhookPayload;
import com.backandwhite.application.service.CjLogisticWebhookHandler;
import com.backandwhite.application.service.CjOrderWebhookHandler;
import com.backandwhite.common.exception.EntityNotFoundException;
import com.backandwhite.infrastructure.configuration.CjWebhookProperties;
import com.backandwhite.infrastructure.db.postgres.entity.CjWebhookLogEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjWebhookLogJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CjWebhookControllerTest {

    @Mock
    private CjWebhookLogJpaRepository webhookLogRepository;
    @Mock
    private CjOrderWebhookHandler orderWebhookHandler;
    @Mock
    private CjLogisticWebhookHandler logisticWebhookHandler;
    @Mock
    private CjWebhookProperties webhookProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CjWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new CjWebhookController(webhookLogRepository, orderWebhookHandler, logisticWebhookHandler,
                objectMapper, webhookProperties);
    }

    private CjWebhookPayload<java.util.Map<String, Object>> orderPayload(String messageId, String messageType) {
        CjWebhookPayload<java.util.Map<String, Object>> p = new CjWebhookPayload<>();
        p.setMessageId(messageId);
        p.setType("ORDER");
        p.setMessageType(messageType);
        java.util.Map<String, Object> params = new HashMap<>();
        params.put("orderId", "o1");
        p.setParams(params);
        return p;
    }

    @ParameterizedTest(name = "receiveOrderWebhook_invalidAuth_throws[secret={0}, token={1}]")
    @CsvSource(value = {"good,bad", "NULL,token", "'  ',token"}, nullValues = "NULL")
    void receiveOrderWebhook_invalidAuth_throws(String configuredSecret, String providedToken) {
        when(webhookProperties.getSecret()).thenReturn(configuredSecret);
        CjWebhookPayload<java.util.Map<String, Object>> payload = orderPayload("m1", "ORDER_CREATED");
        assertThatThrownBy(() -> controller.receiveOrderWebhook(providedToken, payload))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void receiveOrderWebhook_duplicate_returnsOkWithoutSaving() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(true);
        var resp = controller.receiveOrderWebhook("good", orderPayload("m1", "ORDER_CREATED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("result", true);
        verify(webhookLogRepository, never()).save(any(CjWebhookLogEntity.class));
        verify(orderWebhookHandler, never()).handleOrderEvent(any(), any());
    }

    @Test
    void receiveOrderWebhook_split_routesToSplitHandler() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(false);
        var resp = controller.receiveOrderWebhook("good", orderPayload("m1", "ORDERSPLIT_CREATED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(orderWebhookHandler).handleOrderSplitEvent(any(CjOrderSplitWebhookParams.class));
    }

    @Test
    void receiveOrderWebhook_nonSplit_routesToOrderHandler() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(false);
        var resp = controller.receiveOrderWebhook("good", orderPayload("m1", "ORDER_CREATED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(orderWebhookHandler).handleOrderEvent(any(), any(CjOrderWebhookParams.class));
    }

    @Test
    void receiveOrderWebhook_handlerThrows_stillReturnsOk() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(orderWebhookHandler).handleOrderEvent(any(),
                any());
        var resp = controller.receiveOrderWebhook("good", orderPayload("m1", "ORDER_CREATED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void receiveOrderWebhook_nullMessageType_routesToOrderHandler() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId(any())).thenReturn(false);
        CjWebhookPayload<java.util.Map<String, Object>> p = orderPayload("m2", null);
        var resp = controller.receiveOrderWebhook("good", p);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(orderWebhookHandler).handleOrderEvent(any(), any(CjOrderWebhookParams.class));
    }

    @Test
    void receiveLogisticsWebhook_invalidToken_throws() {
        when(webhookProperties.getSecret()).thenReturn("good");
        CjWebhookPayload<java.util.Map<String, Object>> payload = orderPayload("m1", "LOGISTIC_UPDATE");
        assertThatThrownBy(() -> controller.receiveLogisticsWebhook("bad", payload))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void receiveLogisticsWebhook_duplicate_skipsHandler() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(true);
        var resp = controller.receiveLogisticsWebhook("good", orderPayload("m1", "LOGISTIC_UPDATE"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(logisticWebhookHandler, never()).handleLogisticEvent(any());
    }

    @Test
    void receiveLogisticsWebhook_routesToHandler() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(false);
        var resp = controller.receiveLogisticsWebhook("good", orderPayload("m1", "LOGISTIC_UPDATE"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(logisticWebhookHandler).handleLogisticEvent(any(CjLogisticWebhookParams.class));
    }

    @Test
    void receiveLogisticsWebhook_handlerThrows_stillReturnsOk() {
        when(webhookProperties.getSecret()).thenReturn("good");
        when(webhookLogRepository.existsByMessageId("m1")).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(logisticWebhookHandler)
                .handleLogisticEvent(any());
        var resp = controller.receiveLogisticsWebhook("good", orderPayload("m1", "LOGISTIC_UPDATE"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void receiveOrderWebhook_nullMessageId_doesNotSaveLog() {
        when(webhookProperties.getSecret()).thenReturn("good");
        var resp = controller.receiveOrderWebhook("good", orderPayload(null, "ORDER_CREATED"));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(webhookLogRepository, never()).save(any(CjWebhookLogEntity.class));
    }
}
