package com.backandwhite.infrastructure.client.cj.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CjApiResponseDto<T> {
    private String code;
    private Boolean result;
    private String message;
    private T data;
    private String requestId;

    public boolean isSuccess() {
        return Boolean.TRUE.equals(result);
    }
}
