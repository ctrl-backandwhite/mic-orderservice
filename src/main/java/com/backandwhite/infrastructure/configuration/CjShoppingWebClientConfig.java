package com.backandwhite.infrastructure.configuration;

import com.backandwhite.domain.exception.Message;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class CjShoppingWebClientConfig {

    @Bean
    public WebClient cjShoppingWebClient(CjDropshippingProperties properties) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("cj-shopping-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultStatusHandler(
                        status -> status.value() == 429,
                        response -> {
                            throw Message.CJ_RATE_LIMIT.toExternalServiceException();
                        })
                .defaultStatusHandler(
                        status -> status.is4xxClientError() && status.value() != 429,
                        response -> {
                            throw Message.CJ_DATA_ERROR.toExternalServiceException(
                                    "HTTP " + response.statusCode().value());
                        })
                .defaultStatusHandler(
                        status -> status.is5xxServerError(),
                        response -> {
                            throw Message.CJ_DATA_ERROR.toExternalServiceException(
                                    "HTTP " + response.statusCode().value());
                        })
                .build();
    }
}
