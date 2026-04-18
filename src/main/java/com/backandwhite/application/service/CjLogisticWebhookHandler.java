package com.backandwhite.application.service;

import com.backandwhite.api.dto.webhook.CjLogisticWebhookParams;
import com.backandwhite.domain.repository.CjOrderRepository;
import com.backandwhite.infrastructure.db.postgres.entity.CjTrackingEventEntity;
import com.backandwhite.infrastructure.db.postgres.entity.OrderEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjTrackingEventJpaRepository;
import com.backandwhite.infrastructure.db.postgres.repository.OrderJpaRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles LOGISTIC webhook events from CJ Dropshipping.
 * <p>
 * On receipt:
 * <ol>
 * <li>Finds the corresponding order via cjOrderId (from cj_orders) or
 * trackingNumber.</li>
 * <li>Persists each tracking milestone in {@code cj_tracking_events}.</li>
 * <li>Updates {@code orders.tracking_url}, {@code last_mile_carrier},
 * {@code last_mile_track_number} with the latest values.</li>
 * </ol>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CjLogisticWebhookHandler {

    private static final DateTimeFormatter CJ_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CjOrderRepository cjOrderRepository;
    private final OrderJpaRepository orderJpaRepository;
    private final CjTrackingEventJpaRepository trackingEventRepository;

    @Transactional
    public void handleLogisticEvent(CjLogisticWebhookParams params) {
        String trackNumber = params.getTrackingNumber();
        log.info("Processing LOGISTIC webhook for trackingNumber={} status={}", trackNumber, params.getStatus());

        // Resolve our internal orderId from cj_orders using the trackNumber
        String internOrderId = cjOrderRepository.findByCjTrackNumber(trackNumber).map(o -> o.getOrderId()).orElse(null);

        if (internOrderId == null) {
            log.warn("No cj_order found for trackNumber={}. Events will be stored without orderId.", trackNumber);
        }

        // Persist tracking milestone events
        if (params.getEvents() != null) {
            for (CjLogisticWebhookParams.TrackingEvent evt : params.getEvents()) {
                CjTrackingEventEntity entity = CjTrackingEventEntity.builder().orderId(internOrderId)
                        .trackingNumber(trackNumber).trackingStatus(params.getStatus())
                        .statusDescription(params.getStatusDescription()).eventActivity(evt.getActivity())
                        .eventLocation(evt.getLocation()).eventTime(parseEventTime(evt.getEventTime()))
                        .logisticName(params.getLogisticName()).trackingUrl(params.getTrackUrl())
                        .createdAt(Instant.now()).build();
                trackingEventRepository.save(entity);
            }
        }

        // Update the orders table with tracking enrichments
        if (internOrderId != null) {
            orderJpaRepository.findById(internOrderId).ifPresent(order -> {
                enrichOrder(order, params);
                orderJpaRepository.save(order);
            });
        }

        // Update lastWebhookAt on the cj_orders row
        if (internOrderId != null) {
            cjOrderRepository.findByOrderId(internOrderId).ifPresent(cjOrder -> {
                cjOrder.setLastWebhookAt(Instant.now());
                cjOrder.setUpdatedBy("WEBHOOK");
                if (trackNumber != null)
                    cjOrder.setTrackNumber(trackNumber);
                if (params.getLogisticName() != null)
                    cjOrder.setLogisticName(params.getLogisticName());
                cjOrderRepository.save(cjOrder);
            });
        }
    }

    private void enrichOrder(OrderEntity order, CjLogisticWebhookParams params) {
        if (params.getTrackUrl() != null) {
            order.setTrackingUrl(params.getTrackUrl());
        }
        if (params.getLastMileCarrier() != null) {
            order.setLastMileCarrier(params.getLastMileCarrier());
        }
        if (params.getLastTrackNumber() != null) {
            order.setLastMileTrackNumber(params.getLastTrackNumber());
        }
    }

    private Instant parseEventTime(String raw) {
        if (raw == null || raw.isBlank())
            return Instant.now();
        try {
            return LocalDateTime.parse(raw, CJ_DATE_FMT).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse event time '{}': {}", raw, e.getMessage());
            return Instant.now();
        }
    }
}
