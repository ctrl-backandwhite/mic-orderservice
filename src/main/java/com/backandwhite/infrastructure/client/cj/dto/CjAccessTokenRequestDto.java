package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CjAccessTokenRequestDto {
    @JsonProperty("email")
    private String email;
    @JsonProperty("password")
    private String password;

    // apiKey-based constructor
    public CjAccessTokenRequestDto(String apiKey) {
        // CJ v2 uses apiKey directly — stored in apiKey field
        this.email = apiKey;
    }
}
