package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjCreateOrderV3RequestDto {

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("shippingAddress")
    private ShippingAddressDto shippingAddress;

    @JsonProperty("products")
    private List<CjOrderProductItemDto> products;

    @JsonProperty("logisticName")
    private String logisticName;

    @JsonProperty("fromCountryCode")
    private String fromCountryCode;

    @JsonProperty("remark")
    private String remark;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShippingAddressDto {
        @JsonProperty("countryCode")
        private String countryCode;
        @JsonProperty("province")
        private String province;
        @JsonProperty("city")
        private String city;
        @JsonProperty("address")
        private String address;
        @JsonProperty("address2")
        private String address2;
        @JsonProperty("zipCode")
        private String zipCode;
        @JsonProperty("phone")
        private String phone;
        @JsonProperty("firstName")
        private String firstName;
        @JsonProperty("lastName")
        private String lastName;
        @JsonProperty("email")
        private String email;
        @JsonProperty("houseNumber")
        private String houseNumber;
    }
}
