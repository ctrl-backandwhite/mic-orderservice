package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjAddCartResponseDto {

    @JsonProperty("successCount")
    private Integer successCount;

    @JsonProperty("addSuccessOrders")
    private List<String> addSuccessOrders;

    @JsonProperty("interceptOrders")
    private List<InterceptOrder> interceptOrders;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterceptOrder {
        @JsonProperty("cjOrderId")
        private String cjOrderId;
        @JsonProperty("reason")
        private String reason;
    }
}
