package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjCreateOrderV3ResponseDto {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("orderNum")
    private String orderNum;
    @JsonProperty("createTime")
    private String createTime;
    @JsonProperty("productList")
    private List<CjProductInfoListItemDto> productList;
}
