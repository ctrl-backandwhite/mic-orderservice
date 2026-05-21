package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TrackingUrlSigner")
class TrackingUrlSignerTest {

    private TrackingUrlSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        signer = new TrackingUrlSigner();
        Field f = TrackingUrlSigner.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(signer, "test-secret-key-must-be-long-enough-for-hmac");
    }

    @Test
    @DisplayName("signPath produces /tracking/<id>?exp=...&sig=...")
    void signsPath() {
        String path = signer.signPath("ord-1");
        assertThat(path).startsWith("/tracking/ord-1?exp=").contains("&sig=");
    }

    @Test
    @DisplayName("verify accepts a fresh signature")
    void verifyValid() {
        String path = signer.signPath("ord-1", 60);
        long exp = Long.parseLong(path.substring(path.indexOf("exp=") + 4, path.indexOf("&sig=")));
        String sig = path.substring(path.indexOf("sig=") + 4);
        assertThat(signer.verify("ord-1", exp, sig)).isTrue();
    }

    @Test
    @DisplayName("verify rejects expired links")
    void verifyExpired() {
        long expired = Instant.now().getEpochSecond() - 5;
        assertThat(signer.verify("ord-1", expired, "any")).isFalse();
    }

    @Test
    @DisplayName("verify rejects null inputs and tampered signatures")
    void verifyInvalid() {
        long exp = Instant.now().getEpochSecond() + 60;
        assertThat(signer.verify(null, exp, "x")).isFalse();
        assertThat(signer.verify("ord-1", exp, null)).isFalse();
        assertThat(signer.verify("ord-1", exp, "tampered")).isFalse();
    }
}
