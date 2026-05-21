package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PiiEncryptionService")
class PiiEncryptionServiceTest {

    @Test
    @DisplayName("roundtrips plaintext through AES-GCM with a 32-byte key")
    void roundtripsPlaintext() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        String encrypted = service.encrypt("1600 Pennsylvania Ave NW");
        assertThat(encrypted).startsWith("enc:");
        assertThat(service.decrypt(encrypted)).isEqualTo("1600 Pennsylvania Ave NW");
    }

    @Test
    @DisplayName("is a no-op when no key is configured (dev/local)")
    void noOpWhenNoKey() throws Exception {
        PiiEncryptionService service = instantiate("");
        String encrypted = service.encrypt("secret");
        assertThat(encrypted).isEqualTo("secret");
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("produces a different ciphertext on every call thanks to random IV")
    void ivRandomised() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        String a = service.encrypt("same-input");
        String b = service.encrypt("same-input");
        assertThat(a).isNotEqualTo(b);
    }

    private static PiiEncryptionService instantiate(String base64Key) throws Exception {
        PiiEncryptionService service = new PiiEncryptionService();
        Field keyField = PiiEncryptionService.class.getDeclaredField("base64Key");
        keyField.setAccessible(true);
        keyField.set(service, base64Key);
        var method = PiiEncryptionService.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(service);
        return service;
    }

    @org.junit.jupiter.api.Test
    @DisplayName("encrypt returns input unchanged when plaintext is null or empty")
    void encryptShortCircuitsOnNullEmpty() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        org.assertj.core.api.Assertions.assertThat(service.encrypt(null)).isNull();
        org.assertj.core.api.Assertions.assertThat(service.encrypt("")).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("decrypt returns input unchanged when ciphertext is null or empty")
    void decryptShortCircuitsOnNullEmpty() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        org.assertj.core.api.Assertions.assertThat(service.decrypt(null)).isNull();
        org.assertj.core.api.Assertions.assertThat(service.decrypt("")).isEmpty();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("decrypt returns input untouched when not prefixed with 'enc:'")
    void decryptWithoutPrefix() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        org.assertj.core.api.Assertions.assertThat(service.decrypt("plain-text-no-prefix"))
                .isEqualTo("plain-text-no-prefix");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("decrypt returns null when key is missing but ciphertext has 'enc:' prefix")
    void decryptDisabledOnEncrypted() throws Exception {
        PiiEncryptionService service = instantiate("");
        org.assertj.core.api.Assertions.assertThat(service.decrypt("enc:something")).isNull();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("decrypt wraps cipher errors as IllegalStateException for malformed payloads")
    void decryptThrowsOnMalformed() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.decrypt("enc:not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("decryption failed");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("init throws IllegalStateException when key is not 32 bytes")
    void initRejectsBadKey() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> instantiate(Base64.getEncoder().encodeToString(new byte[16])))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("isEnabled returns true when a valid key is configured")
    void isEnabledTrue() throws Exception {
        PiiEncryptionService service = instantiate(Base64.getEncoder().encodeToString(new byte[32]));
        org.assertj.core.api.Assertions.assertThat(service.isEnabled()).isTrue();
    }
}
