package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjProductInfoListItemDto {
    @JsonProperty("vid")
    private String vid;
    @JsonProperty("pid")
    private String pid;
    @JsonProperty("productNameEn")
    private String productNameEn;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("unitPrice")
    private String unitPrice;
    @JsonProperty("totalPrice")
    private String totalPrice;
    @JsonProperty("trackNumber")
    private String trackNumber;
    @JsonProperty("logisticName")
    private String logisticName;
    @JsonProperty("shipmentStatus")
    private String shipmentStatus;
}
