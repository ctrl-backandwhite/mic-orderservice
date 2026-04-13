package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjBalanceResponseDto {
    @JsonProperty("balance")
    private String balance;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("creditBalance")
    private String creditBalance;
}
