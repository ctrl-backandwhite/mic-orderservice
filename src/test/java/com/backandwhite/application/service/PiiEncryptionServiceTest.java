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
}
