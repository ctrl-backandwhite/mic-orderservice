package com.backandwhite.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code app.cj.webhook.*} settings. The {@code secret} acts as the
 * shared token embedded in the webhook path — requests arriving at paths that
 * do not carry the configured value are rejected with 404 to keep the endpoint
 * invisible to scanners.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.cj.webhook")
public class CjWebhookProperties {
    private String baseUrl;
    private String secret;
}
