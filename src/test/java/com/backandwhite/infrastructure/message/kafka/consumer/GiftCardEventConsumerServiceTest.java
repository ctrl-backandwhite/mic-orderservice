package com.backandwhite.infrastructure.message.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.core.kafka.avro.GiftCardPurchasedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GiftCardEventConsumerServiceTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private GiftCardEventConsumerService consumer;

    private GiftCardPurchasedEvent fullEvent() {
        return GiftCardPurchasedEvent.newBuilder().setGiftCardId("gc-1").setCode("CODE-1").setBuyerId("buyer-1")
                .setBuyerName("Buyer Name").setBuyerEmail("buyer@e.com").setRecipientName("Recipient")
                .setRecipientEmail("rec@e.com").setAmount("100.00").setCurrency("USD").setMessage("happy birthday")
                .setExpiryDate("2026-12-31").setDesignId("design-1").setTimestamp("2026-01-01T00:00:00Z").build();
    }

    @Test
    void onGiftCardPurchased_invokesUseCaseWithAllFields() {
        consumer.onGiftCardPurchased(fullEvent());

        verify(orderUseCase).createGiftCardOrder("gc-1", "CODE-1", "buyer-1", "buyer@e.com", "Buyer Name", "100.00",
                "USD", "Recipient", "rec@e.com", "happy birthday");
    }

    @Test
    void onGiftCardPurchased_swallowsException() {
        when(orderUseCase.createGiftCardOrder(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db down"));

        // Should not throw out of the listener.
        consumer.onGiftCardPurchased(fullEvent());

        verify(orderUseCase).createGiftCardOrder(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onGiftCardPurchased_minimalFields() {
        GiftCardPurchasedEvent event = GiftCardPurchasedEvent.newBuilder().setGiftCardId("gc-2").setCode("CODE-2")
                .setBuyerId("buyer-2").setBuyerName("B").setBuyerEmail("buyer2@e.com").setRecipientName("R")
                .setRecipientEmail("rec2@e.com").setAmount("50.00").setCurrency("USD").setMessage("hi")
                .setExpiryDate("2026-12-31").setDesignId("d-2").setTimestamp("2026-01-01T00:00:00Z").build();

        consumer.onGiftCardPurchased(event);

        verify(orderUseCase).createGiftCardOrder("gc-2", "CODE-2", "buyer-2", "buyer2@e.com", "B", "50.00", "USD", "R",
                "rec2@e.com", "hi");
    }
}
