package com.backandwhite.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracking information returned by CJ's trackInfo API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CjTrackInfo {

    private String trackingNumber;
    private String logisticName;
    private String trackingFrom;
    private String trackingTo;
    private String deliveryDay;
    private String deliveryTime;
    private String trackingStatus;
    private String lastMileCarrier;
    private String lastTrackNumber;
}
