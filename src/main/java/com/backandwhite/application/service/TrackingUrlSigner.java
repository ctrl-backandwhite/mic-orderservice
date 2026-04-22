package com.backandwhite.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues short-lived HMAC-signed links that let the customer open the
 * order-tracking page from a confirmation / shipment email without logging in.
 *
 * <p>
 * Link shape: {@code /tracking/<orderId>?exp=<unix>&sig=<hmac>}. The frontend
 * detects the query params and calls the public tracking endpoint with them
 * verbatim; this service validates that the signature matches and that the
 * window is still open.
 *
 * <p>
 * Signature covers {@code orderId + "|" + exp} with HMAC-SHA256 using the JWT
 * secret (reused from InvoicePdfUrlSigner so we don't need another config
 * entry). TTL defaults to 60 days — matches the typical shipping / return
 * windows.
 */
@Log4j2
@Service
public class TrackingUrlSigner {

    private static final long DEFAULT_TTL_SECONDS = 60L * 60 * 24 * 60; // 60 days
    private static final String ALGO = "HmacSHA256";

    @Value("${app.jwt.secret:${APP_JWT_SECRET:local-secret-key-change-me-in-production-must-be-256-bits-long}}")
    private String secret;

    public String signPath(String orderId) {
        return signPath(orderId, DEFAULT_TTL_SECONDS);
    }

    public String signPath(String orderId, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String sig = sign(orderId, exp);
        return "/tracking/" + orderId + "?exp=" + exp + "&sig=" + sig;
    }

    public boolean verify(String orderId, long exp, String sig) {
        if (orderId == null || sig == null)
            return false;
        if (Instant.now().getEpochSecond() > exp) {
            log.debug("::> public tracking link expired (orderId={}, exp={})", orderId, exp);
            return false;
        }
        String expected = sign(orderId, exp);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String orderId, long exp) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            byte[] digest = mac.doFinal((orderId + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign tracking URL", e);
        }
    }
}
