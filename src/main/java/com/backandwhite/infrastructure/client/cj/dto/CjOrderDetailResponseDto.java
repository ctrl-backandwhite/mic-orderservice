package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjOrderDetailResponseDto {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("orderNum")
    private String orderNum;
    @JsonProperty("orderStatus")
    private String orderStatus;
    @JsonProperty("trackNumber")
    private String trackNumber;
    @JsonProperty("logisticName")
    private String logisticName;
    @JsonProperty("shipmentOrderId")
    private String shipmentOrderId;
    @JsonProperty("createTime")
    private String createTime;
    @JsonProperty("productList")
    private List<CjProductInfoListItemDto> productList;
}
