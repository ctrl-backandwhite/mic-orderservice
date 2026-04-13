package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjAccessTokenDataDto {
    @JsonProperty("accessToken")
    private String accessToken;
    @JsonProperty("accessTokenExpiryDate")
    private String accessTokenExpiryDate;
    @JsonProperty("refreshToken")
    private String refreshToken;
    @JsonProperty("refreshTokenExpiryDate")
    private String refreshTokenExpiryDate;
}
