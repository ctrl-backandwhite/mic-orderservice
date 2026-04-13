package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjTrackInfoResponseDto {

    @JsonProperty("trackingNumber")
    private String trackingNumber;

    @JsonProperty("logisticName")
    private String logisticName;

    @JsonProperty("trackingFrom")
    private String trackingFrom;

    @JsonProperty("trackingTo")
    private String trackingTo;

    @JsonProperty("deliveryDay")
    private String deliveryDay;

    @JsonProperty("deliveryTime")
    private String deliveryTime;

    @JsonProperty("trackingStatus")
    private String trackingStatus;

    @JsonProperty("lastMileCarrier")
    private String lastMileCarrier;

    @JsonProperty("lastTrackNumber")
    private String lastTrackNumber;
}
