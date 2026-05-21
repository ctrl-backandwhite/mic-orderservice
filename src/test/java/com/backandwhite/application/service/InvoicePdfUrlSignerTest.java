package com.backandwhite.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvoicePdfUrlSigner")
class InvoicePdfUrlSignerTest {

    private InvoicePdfUrlSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        signer = new InvoicePdfUrlSigner();
        Field f = InvoicePdfUrlSigner.class.getDeclaredField("secret");
        f.setAccessible(true);
        f.set(signer, "test-secret-key-must-be-long-enough-for-hmac");
    }

    @Test
    @DisplayName("signPath produces a path with exp + sig parameters")
    void signsPath() {
        String path = signer.signPath("ord-1");
        assertThat(path).startsWith("/api/v1/invoices/public/order/ord-1/pdf?exp=").contains("&sig=");
    }

    @Test
    @DisplayName("verify returns true for a freshly signed path")
    void verifyValid() {
        long exp = Instant.now().getEpochSecond() + 60;
        String path = signer.signPath("ord-1", 60);
        String sig = path.substring(path.indexOf("sig=") + 4);
        long parsedExp = Long.parseLong(path.substring(path.indexOf("exp=") + 4, path.indexOf("&sig=")));
        assertThat(signer.verify("ord-1", parsedExp, sig)).isTrue();
        assertThat(parsedExp).isCloseTo(exp, org.assertj.core.data.Offset.offset(2L));
    }

    @Test
    @DisplayName("verify returns false on tampered signature")
    void verifyTampered() {
        long exp = Instant.now().getEpochSecond() + 60;
        assertThat(signer.verify("ord-1", exp, "ZZZ")).isFalse();
    }

    @Test
    @DisplayName("verify returns false for expired links")
    void verifyExpired() {
        long expired = Instant.now().getEpochSecond() - 10;
        String fakeSig = "anything";
        assertThat(signer.verify("ord-1", expired, fakeSig)).isFalse();
    }

    @Test
    @DisplayName("verify returns false for null orderId or sig")
    void verifyNulls() {
        long exp = Instant.now().getEpochSecond() + 60;
        assertThat(signer.verify(null, exp, "x")).isFalse();
        assertThat(signer.verify("ord-1", exp, null)).isFalse();
    }
}
