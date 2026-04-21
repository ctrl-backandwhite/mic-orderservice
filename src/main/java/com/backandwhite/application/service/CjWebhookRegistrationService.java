package com.backandwhite.application.service;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.infrastructure.configuration.CjWebhookProperties;
import com.backandwhite.infrastructure.db.postgres.entity.AppSettingEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AppSettingJpaRepository;
import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Fase 2 of the CJ integration plan — at boot, make sure CJ has our webhook
 * callback URL registered. Compares the SHA-256 of the would-be registration
 * against what's in {@code app_settings} to skip redundant calls. Treats CJ's
 * {@code 1606000} ("already registered") as success inside the client.
 *
 * <p>
 * Runs only when {@code app.cj.enabled=true} AND a base URL + secret are
 * configured, so local dev and tests do not touch CJ.
 * </p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cj.enabled", havingValue = "true")
public class CjWebhookRegistrationService {

    private static final String SETTING_KEY = "cj.webhook.registration.hash";

    private final CjShoppingPort cjShopping;
    private final CjWebhookProperties webhookProperties;
    private final AppSettingJpaRepository settingsRepository;

    @PostConstruct
    @Async
    public void registerOnStartup() {
        String baseUrl = webhookProperties.getBaseUrl();
        String secret = webhookProperties.getSecret();
        if (baseUrl == null || baseUrl.isBlank() || secret == null || secret.isBlank()) {
            log.info("::> CJ webhook registration skipped — baseUrl or secret missing");
            return;
        }

        String callbackUrl = baseUrl.replaceAll("/+$", "") + "/api/v1/cj/webhook/" + secret + "/order";
        String desiredHash = sha256(callbackUrl);

        String currentHash = settingsRepository.findById(SETTING_KEY).map(AppSettingEntity::getValue).orElse(null);
        if (desiredHash.equals(currentHash)) {
            log.info("::> CJ webhook already registered (hash match) — skipping /webhook/set");
            return;
        }

        try {
            cjShopping.registerWebhookUrl(callbackUrl);
            settingsRepository.save(
                    AppSettingEntity.builder().key(SETTING_KEY).value(desiredHash).updatedAt(Instant.now()).build());
            log.info("::> CJ webhook registered successfully: {}", callbackUrl);
        } catch (Exception e) {
            log.error("::> CJ webhook registration failed — will retry next boot: {}", e.getMessage());
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
