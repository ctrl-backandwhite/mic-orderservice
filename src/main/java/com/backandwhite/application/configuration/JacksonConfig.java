package com.backandwhite.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a Jackson 2.x {@link ObjectMapper} bean so that components using the
 * {@code com.fasterxml.jackson.databind.ObjectMapper} type can be autowired.
 * Spring Boot 4 auto-configures a {@code tools.jackson.databind.ObjectMapper}
 * (Jackson 3.x), but Jackson 2.x is still on the classpath via Kafka transitive
 * dependencies; this bean bridges the gap.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
