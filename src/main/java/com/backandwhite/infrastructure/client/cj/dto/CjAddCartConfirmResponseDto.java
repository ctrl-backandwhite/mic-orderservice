package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjAddCartConfirmResponseDto {

    @JsonProperty("submitSuccess")
    private Boolean submitSuccess;

    @JsonProperty("shipmentsId")
    private String shipmentsId;

    @JsonProperty("interceptOrders")
    private List<CjAddCartResponseDto.InterceptOrder> interceptOrders;
}
