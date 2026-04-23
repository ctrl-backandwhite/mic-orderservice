package com.backandwhite.infrastructure.message.kafka.consumer;

import com.backandwhite.application.usecase.OrderUseCase;
import com.backandwhite.common.constants.AppConstants;
import com.backandwhite.core.kafka.avro.GiftCardPurchasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes gift card purchase events and creates a synthetic order + fiscal
 * invoice so the buyer receives the invoice email through the standard
 * notification pipeline.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class GiftCardEventConsumerService {

    private final OrderUseCase orderUseCase;

    @KafkaListener(topics = AppConstants.KAFKA_TOPIC_GIFT_CARD_PURCHASED, groupId = AppConstants.KAFKA_GROUP_ORDER, containerFactory = "avroKafkaListenerContainerFactory")
    public void onGiftCardPurchased(GiftCardPurchasedEvent event) {
        String giftCardId = str(event.getGiftCardId());
        String buyerEmail = str(event.getBuyerEmail());
        log.info("::> Received giftcard.purchased: giftCardId={}, code={}, buyerEmail={}", giftCardId,
                str(event.getCode()), buyerEmail);
        try {
            orderUseCase.createGiftCardOrder(giftCardId, str(event.getCode()), str(event.getBuyerId()), buyerEmail,
                    str(event.getBuyerName()), str(event.getAmount()), str(event.getCurrency()),
                    str(event.getRecipientName()), str(event.getRecipientEmail()), str(event.getMessage()));
        } catch (Exception e) {
            log.error("::> Failed processing giftcard.purchased for giftCardId={}: {}", giftCardId, e.getMessage(), e);
        }
    }

    private String str(CharSequence cs) {
        return cs != null ? cs.toString() : null;
    }
}
