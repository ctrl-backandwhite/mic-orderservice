package com.backandwhite.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cj-dropshipping")
public class CjDropshippingProperties {
    private String baseUrl;
    private String apiKey;
}
