package com.backandwhite.api.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Params block for LOGISTIC webhook events from CJ. Carries detailed
 * tracking/milestone information for a shipment.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjLogisticWebhookParams {

    private String trackingNumber;
    private String logisticName;

    /** High-level tracking status, e.g. "IN_TRANSIT", "DELIVERED". */
    private String status;

    /** Human-readable status description. */
    private String statusDescription;

    /** Public tracking URL. */
    private String trackUrl;

    /** Last-mile carrier code or name. */
    private String lastMileCarrier;

    /** Last-mile tracking number. */
    private String lastTrackNumber;

    /** Ordered list of tracking milestones (newest first). */
    private List<TrackingEvent> events;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingEvent {
        private String activity;
        private String location;
        /** Event time from CJ, format "yyyy-MM-dd HH:mm:ss". */
        private String eventTime;
    }
}
