package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjGenerateParentOrderResponseDto {

    @JsonProperty("payId")
    private String payId;

    @JsonProperty("orderMoney")
    private BigDecimal orderMoney;

    @JsonProperty("payExpireTime")
    private String payExpireTime;

    @JsonProperty("paymentInformation")
    private PaymentInformation paymentInformation;

    @JsonProperty("interceptOrders")
    private List<CjAddCartResponseDto.InterceptOrder> interceptOrders;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentInformation {
        @JsonProperty("actualPayment")
        private BigDecimal actualPayment;

        @JsonProperty("balanceDeduction")
        private BigDecimal balanceDeduction;

        @JsonProperty("postage")
        private BigDecimal postage;

        @JsonProperty("productAmount")
        private BigDecimal productAmount;

        @JsonProperty("taxFee")
        private BigDecimal taxFee;
    }
}
