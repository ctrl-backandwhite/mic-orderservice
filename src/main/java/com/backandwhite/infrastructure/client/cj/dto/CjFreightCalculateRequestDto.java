package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjFreightCalculateRequestDto {

    @JsonProperty("startCountryCode")
    private String startCountryCode;

    @JsonProperty("endCountryCode")
    private String endCountryCode;

    @JsonProperty("products")
    private List<ProductItem> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductItem {
        @JsonProperty("vid")
        private String vid;

        @JsonProperty("quantity")
        private Integer quantity;
    }
}
