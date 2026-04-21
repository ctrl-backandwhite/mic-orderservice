package com.backandwhite.application.service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fase 15 — AES-256-GCM encryption helper for at-rest PII (address lines,
 * phone, email captured at order time). The key is injected via
 * {@code app.security.pii.key} (base64-encoded 32 bytes) and can be rotated by
 * redeploying with a new key and running an offline re-encryption job.
 *
 * <p>
 * Every ciphertext prefixes a 12-byte IV so two encryptions of the same
 * plaintext produce different outputs, defeating dictionary attacks against the
 * DB.
 * </p>
 */
@Log4j2
@Service
public class PiiEncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    @Value("${app.security.pii.key:}")
    private String base64Key;

    private SecretKey secretKey;
    private boolean enabled;

    @PostConstruct
    void init() {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("::> PII encryption DISABLED — app.security.pii.key not configured. "
                    + "Safe for local/dev, NOT safe for prod (Fase 15).");
            this.enabled = false;
            return;
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.security.pii.key must decode to exactly 32 bytes (AES-256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.enabled = true;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (!enabled) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return "enc:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("PII encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        if (!ciphertext.startsWith("enc:")) {
            return ciphertext;
        }
        if (!enabled) {
            log.warn("::> Found encrypted PII but decryption key is not configured");
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(4));
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PII decryption failed", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
