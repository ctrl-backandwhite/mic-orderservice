package com.backandwhite.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalServiceException")
class ExternalServiceExceptionTest {

    @Test
    @DisplayName("constructor with code + detail-list keeps both fields")
    void codeAndDetails() {
        ExternalServiceException ex = new ExternalServiceException("CJ-1", List.of("api down", "retry later"));
        assertThat(ex.getCode()).isEqualTo("CJ-1");
        assertThat(ex.getDetail()).containsExactly("api down", "retry later");
    }

    @Test
    @DisplayName("constructor with code + message keeps message and code")
    void codeAndMessage() {
        ExternalServiceException ex = new ExternalServiceException("CJ-2", "single message");
        assertThat(ex.getMessage()).isEqualTo("single message");
        assertThat(ex.getCode()).isEqualTo("CJ-2");
    }

    @Test
    @DisplayName("3-arg constructor seeds message + code + details")
    void messageCodeAndDetails() {
        ExternalServiceException ex = new ExternalServiceException("primary", "CJ-3", List.of("d1", "d2"));
        assertThat(ex.getMessage()).isEqualTo("primary");
        assertThat(ex.getCode()).isEqualTo("CJ-3");
        assertThat(ex.getDetail()).containsExactly("d1", "d2");
    }
}
