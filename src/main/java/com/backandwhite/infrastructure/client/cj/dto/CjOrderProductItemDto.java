package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjOrderProductItemDto {
    @JsonProperty("vid")
    private String vid;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("shippingName")
    private String shippingName;
}
