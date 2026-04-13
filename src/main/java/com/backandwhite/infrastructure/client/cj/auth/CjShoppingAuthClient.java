package com.backandwhite.infrastructure.client.cj.auth;

import com.backandwhite.domain.exception.ExternalServiceException;
import com.backandwhite.infrastructure.client.cj.dto.CjAccessTokenDataDto;
import com.backandwhite.infrastructure.client.cj.dto.CjAccessTokenRequestDto;
import com.backandwhite.infrastructure.client.cj.dto.CjApiResponseDto;
import com.backandwhite.infrastructure.client.cj.dto.CjRefreshTokenRequestDto;
import com.backandwhite.infrastructure.configuration.CjDropshippingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;

import static com.backandwhite.domain.exception.Message.CJ_TOKEN_ERROR;

/**
 * Handles CJ Dropshipping authentication endpoints exclusively.
 * Kept separate from
 * {@link com.backandwhite.infrastructure.client.cj.CjShoppingClient}
 * to avoid a circular dependency with {@link CjShoppingTokenManager}.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class CjShoppingAuthClient {

    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(15);

    @Qualifier("cjShoppingWebClient")
    private final WebClient cjShoppingWebClient;
    private final CjDropshippingProperties properties;

    public CjAccessTokenDataDto requestNewToken() {
        log.info("Requesting CJ Shopping access token...");
        try {
            CjApiResponseDto<CjAccessTokenDataDto> response = cjShoppingWebClient.post()
                    .uri("/authentication/getAccessToken")
                    .bodyValue(new CjAccessTokenRequestDto(properties.getApiKey()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjAccessTokenDataDto>>() {
                    })
                    .timeout(AUTH_TIMEOUT)
                    .block();

            if (response == null || response.getData() == null) {
                throw CJ_TOKEN_ERROR.toExternalServiceException();
            }
            log.info("CJ Shopping access token obtained. Expiry: {}", response.getData().getAccessTokenExpiryDate());
            return response.getData();

        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientException e) {
            log.error("CJ Shopping token request failed (WebClient): {}", e.getMessage(), e);
            throw CJ_TOKEN_ERROR.toExternalServiceException();
        } catch (Exception e) {
            log.error("CJ Shopping token request failed (unexpected): {}", e.getMessage(), e);
            throw CJ_TOKEN_ERROR.toExternalServiceException();
        }
    }

    public CjAccessTokenDataDto refreshAccessToken(String refreshToken) {
        log.info("Refreshing CJ Shopping access token...");
        try {
            CjApiResponseDto<CjAccessTokenDataDto> response = cjShoppingWebClient.post()
                    .uri("/authentication/refreshAccessToken")
                    .bodyValue(new CjRefreshTokenRequestDto(refreshToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CjApiResponseDto<CjAccessTokenDataDto>>() {
                    })
                    .timeout(AUTH_TIMEOUT)
                    .block();

            if (response == null || response.getData() == null) {
                throw CJ_TOKEN_ERROR.toExternalServiceException();
            }
            log.info("CJ Shopping access token refreshed. New expiry: {}",
                    response.getData().getAccessTokenExpiryDate());
            return response.getData();

        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientException e) {
            log.error("CJ Shopping token refresh failed (WebClient): {}", e.getMessage(), e);
            throw CJ_TOKEN_ERROR.toExternalServiceException();
        } catch (Exception e) {
            log.error("CJ Shopping token refresh failed (unexpected): {}", e.getMessage(), e);
            throw CJ_TOKEN_ERROR.toExternalServiceException();
        }
    }
}
