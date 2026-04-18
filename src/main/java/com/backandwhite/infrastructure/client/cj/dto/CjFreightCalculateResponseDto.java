package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjFreightCalculateResponseDto {

    @JsonProperty("logisticName")
    private String logisticName;

    @JsonProperty("logisticPrice")
    private BigDecimal logisticPrice;

    @JsonProperty("logisticPriceCn")
    private BigDecimal logisticPriceCn;

    @JsonProperty("logisticAging")
    private String logisticAging;

    @JsonProperty("taxesFee")
    private BigDecimal taxesFee;

    @JsonProperty("clearanceOperationFee")
    private BigDecimal clearanceOperationFee;
}
