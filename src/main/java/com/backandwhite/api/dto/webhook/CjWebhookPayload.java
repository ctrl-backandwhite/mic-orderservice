package com.backandwhite.api.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for all CJ webhook payloads. CJ POSTs JSON like:
 * 
 * <pre>
 * {
 *   "messageId": "uuid",
 *   "type": "ORDER",
 *   "messageType": "ORDER_CREATED",
 *   "createTime": "2024-01-01 12:00:00",
 *   "params": { ... }
 * }
 * </pre>
 *
 * @param <T>
 *            the concrete params type
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjWebhookPayload<T> {

    /** CJ-generated idempotency key — must be stored to deduplicate. */
    private String messageId;

    /** Top-level category: ORDER, LOGISTIC, ORDERSPLIT, etc. */
    private String type;

    /** More specific event type, e.g. ORDER_CREATED, SHIPPING, etc. */
    private String messageType;

    /** Timestamp from CJ, format "yyyy-MM-dd HH:mm:ss". */
    private String createTime;

    /** Event-specific payload — deserialized by the controller via Jackson. */
    private T params;
}
