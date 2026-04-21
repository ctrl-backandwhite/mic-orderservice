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
 * Fase 9 — issues short-lived HMAC-signed links that let the customer download
 * their invoice PDF from the confirmation email without logging in.
 *
 * <p>
 * The link looks like:<br>
 * {@code /api/v1/invoices/public/order/<orderId>/pdf?exp=<unix>&sig=<hmac>}
 *
 * <p>
 * The signature covers {@code orderId + "|" + exp} with HMAC-SHA256 using the
 * app JWT secret (reused to avoid an extra config). TTL is 30 days, so the
 * customer can still fetch a printout weeks after paying; an expired link
 * returns 404 to keep the endpoint invisible to probes.
 * </p>
 */
@Log4j2
@Service
public class InvoicePdfUrlSigner {

    private static final long DEFAULT_TTL_SECONDS = 60L * 60 * 24 * 30; // 30 days
    private static final String ALGO = "HmacSHA256";

    @Value("${app.jwt.secret:${APP_JWT_SECRET:local-secret-key-change-me-in-production-must-be-256-bits-long}}")
    private String secret;

    /**
     * Returns a relative URL path with exp + sig params appended. Prepend with the
     * store base URL when embedding in an email.
     */
    public String signPath(String orderId) {
        return signPath(orderId, DEFAULT_TTL_SECONDS);
    }

    public String signPath(String orderId, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String sig = sign(orderId, exp);
        return "/api/v1/invoices/public/order/" + orderId + "/pdf?exp=" + exp + "&sig=" + sig;
    }

    /**
     * Verifies the signature matches and the link hasn't expired. Constant-time
     * comparison to avoid timing-leak on the HMAC.
     */
    public boolean verify(String orderId, long exp, String sig) {
        if (orderId == null || sig == null)
            return false;
        if (Instant.now().getEpochSecond() > exp) {
            log.debug("::> invoice pdf link expired (orderId={}, exp={})", orderId, exp);
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
            throw new IllegalStateException("Failed to sign invoice URL", e);
        }
    }
}
