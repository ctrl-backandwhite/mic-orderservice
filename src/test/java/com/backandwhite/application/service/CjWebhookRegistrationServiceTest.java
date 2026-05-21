package com.backandwhite.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.application.port.out.CjShoppingPort;
import com.backandwhite.infrastructure.configuration.CjWebhookProperties;
import com.backandwhite.infrastructure.db.postgres.entity.AppSettingEntity;
import com.backandwhite.infrastructure.db.postgres.repository.AppSettingJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CjWebhookRegistrationService")
class CjWebhookRegistrationServiceTest {

    @Mock
    private CjShoppingPort cjShopping;
    @Mock
    private CjWebhookProperties webhookProperties;
    @Mock
    private AppSettingJpaRepository settingsRepository;

    @InjectMocks
    private CjWebhookRegistrationService service;

    @Test
    @DisplayName("skips registration when baseUrl is missing")
    void skipsWhenNoBaseUrl() {
        when(webhookProperties.getBaseUrl()).thenReturn(null);
        when(webhookProperties.getSecret()).thenReturn("secret");
        service.registerOnStartup();
        verify(cjShopping, never()).registerWebhookUrl(anyString());
    }

    @Test
    @DisplayName("skips registration when secret is blank")
    void skipsWhenBlankSecret() {
        when(webhookProperties.getBaseUrl()).thenReturn("https://x");
        when(webhookProperties.getSecret()).thenReturn(" ");
        service.registerOnStartup();
        verify(cjShopping, never()).registerWebhookUrl(anyString());
    }

    @Test
    @DisplayName("registers and persists hash on first run")
    void registersWhenNoHash() {
        when(webhookProperties.getBaseUrl()).thenReturn("https://x.com/");
        when(webhookProperties.getSecret()).thenReturn("sec");
        when(settingsRepository.findById(anyString())).thenReturn(Optional.empty());
        service.registerOnStartup();
        verify(cjShopping).registerWebhookUrl("https://x.com/api/v1/cj/webhook/sec/order");
        verify(settingsRepository).save(any(AppSettingEntity.class));
    }

    @Test
    @DisplayName("skips remote call when stored hash matches expected")
    void skipsWhenHashMatches() {
        when(webhookProperties.getBaseUrl()).thenReturn("https://x.com");
        when(webhookProperties.getSecret()).thenReturn("sec");
        // sha256 of https://x.com/api/v1/cj/webhook/sec/order
        AppSettingEntity entity = AppSettingEntity.builder().key("cj.webhook.registration.hash")
                .value(sha256("https://x.com/api/v1/cj/webhook/sec/order")).build();
        when(settingsRepository.findById(anyString())).thenReturn(Optional.of(entity));
        service.registerOnStartup();
        verify(cjShopping, never()).registerWebhookUrl(anyString());
    }

    @Test
    @DisplayName("registration failure is swallowed and not persisted")
    void registrationFailureSwallowed() {
        when(webhookProperties.getBaseUrl()).thenReturn("https://x.com");
        when(webhookProperties.getSecret()).thenReturn("sec");
        when(settingsRepository.findById(anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("boom")).when(cjShopping).registerWebhookUrl(anyString());
        service.registerOnStartup();
        verify(settingsRepository, never()).save(any());
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
