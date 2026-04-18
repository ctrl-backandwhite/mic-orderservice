package com.backandwhite.infrastructure.client.cj.auth;

import com.backandwhite.infrastructure.client.cj.dto.CjAccessTokenDataDto;
import com.backandwhite.infrastructure.db.postgres.entity.CjTokenEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjTokenJpaRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Manages the CJ Shopping API token lifecycle with DB persistence. On startup,
 * loads the last token from DB (survives restarts). Proactively refreshes the
 * token 30 minutes before expiry. Hourly scheduled refresh via @Scheduled.
 */
@Log4j2
@Component
public class CjShoppingTokenManager {

    private static final String SINGLETON_ID = "SINGLETON";
    private static final DateTimeFormatter CJ_DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final long TOKEN_REQUEST_COOLDOWN_SECONDS = 5 * 60;
    private static final long PROACTIVE_REFRESH_MINUTES = 30;

    private final CjShoppingAuthClient cjShoppingAuthClient;
    private final CjTokenJpaRepository tokenRepository;
    private final ReentrantLock lock = new ReentrantLock();

    private String cachedAccessToken;
    private String cachedRefreshToken;
    private Instant accessTokenExpiry;
    private Instant refreshTokenExpiry;
    private Instant lastTokenRequestTime;
    private boolean loadedFromDb = false;

    public CjShoppingTokenManager(CjShoppingAuthClient cjShoppingAuthClient, CjTokenJpaRepository tokenRepository) {
        this.cjShoppingAuthClient = cjShoppingAuthClient;
        this.tokenRepository = tokenRepository;
    }

    public String getValidAccessToken() {
        lock.lock();
        try {
            if (!loadedFromDb) {
                loadFromDatabase();
                loadedFromDb = true;
            }

            if (cachedAccessToken != null && !isExpired()) {
                if (isAboutToExpire()) {
                    log.info("CJ Shopping token about to expire in <{} min, attempting proactive refresh...",
                            PROACTIVE_REFRESH_MINUTES);
                    try {
                        doRefresh();
                    } catch (Exception e) {
                        log.warn("Proactive refresh failed ({}), continuing with current token", e.getMessage());
                    }
                } else {
                    long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), accessTokenExpiry);
                    log.debug("Using persisted CJ Shopping access token (expires in {} min)", minutesLeft);
                }
                return cachedAccessToken;
            }

            if (cachedRefreshToken != null && !isRefreshTokenExpired()) {
                log.info("CJ Shopping access token expired, attempting refresh...");
                try {
                    return doRefresh();
                } catch (Exception e) {
                    log.warn("Refresh failed ({}), requesting new token...", e.getMessage());
                }
            }

            log.info("No valid CJ Shopping access token, requesting new token...");
            return doNewToken();
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedRate = 3_600_000, initialDelay = 15_000)
    public void scheduledRefresh() {
        lock.lock();
        try {
            log.info("══════ CJ Shopping Token Scheduled Refresh ══════");
            if (!loadedFromDb) {
                loadFromDatabase();
                loadedFromDb = true;
            }
            if (cachedAccessToken == null) {
                log.info("No token in DB, requesting initial token...");
                doNewToken();
                logTokenStatus();
                log.info("═══════════════════════════════════════════════");
                return;
            }
            logTokenStatus();
            if (cachedRefreshToken != null && !isRefreshTokenExpired()) {
                log.info("Refreshing CJ Shopping access token using refresh token...");
                try {
                    doRefresh();
                    log.info("CJ Shopping token refreshed and persisted.");
                } catch (Exception e) {
                    log.warn("Refresh failed ({}), requesting new token...", e.getMessage());
                    doNewToken();
                }
            } else {
                log.info("Refresh token expired or unavailable, requesting new token...");
                doNewToken();
            }
            logTokenStatus();
            log.info("═══════════════════════════════════════════════");
        } finally {
            lock.unlock();
        }
    }

    private void loadFromDatabase() {
        try {
            tokenRepository.findById(SINGLETON_ID).ifPresentOrElse(entity -> {
                this.cachedAccessToken = entity.getAccessToken();
                this.cachedRefreshToken = entity.getRefreshToken();
                this.accessTokenExpiry = entity.getAccessTokenExpiry();
                this.refreshTokenExpiry = entity.getRefreshTokenExpiry();
                this.lastTokenRequestTime = entity.getLastTokenRequestTime();
                if (cachedAccessToken != null && !isExpired()) {
                    long min = ChronoUnit.MINUTES.between(Instant.now(), accessTokenExpiry);
                    log.info("Loaded valid CJ Shopping token from DB (expires in {} min)", min);
                } else if (cachedAccessToken != null) {
                    log.info("Loaded expired CJ Shopping token from DB, will renew");
                }
            }, () -> log.info("No CJ Shopping token in DB"));
        } catch (Exception e) {
            log.warn("Could not load CJ Shopping token from DB: {}. Will request new.", e.getMessage());
        }
    }

    private void saveToDatabase() {
        try {
            CjTokenEntity entity = CjTokenEntity.builder().id(SINGLETON_ID).accessToken(cachedAccessToken)
                    .refreshToken(cachedRefreshToken).accessTokenExpiry(accessTokenExpiry)
                    .refreshTokenExpiry(refreshTokenExpiry).lastTokenRequestTime(lastTokenRequestTime).build();
            tokenRepository.save(entity);
            log.debug("CJ Shopping token persisted to DB");
        } catch (Exception e) {
            log.error("Failed to persist CJ Shopping token to DB: {}", e.getMessage());
        }
    }

    private String doRefresh() {
        CjAccessTokenDataDto data = cjShoppingAuthClient.refreshAccessToken(cachedRefreshToken);
        storeTokenData(data);
        return cachedAccessToken;
    }

    private String doNewToken() {
        if (lastTokenRequestTime != null) {
            long elapsed = ChronoUnit.SECONDS.between(lastTokenRequestTime, Instant.now());
            if (elapsed < TOKEN_REQUEST_COOLDOWN_SECONDS) {
                long remaining = TOKEN_REQUEST_COOLDOWN_SECONDS - elapsed;
                if (cachedAccessToken != null) {
                    log.warn("CJ Shopping token cooldown active ({} s remaining). Reusing cached token.", remaining);
                    return cachedAccessToken;
                }
                throw new RuntimeException(
                        "CJ Shopping token cooldown: must wait " + remaining + "s before requesting new token");
            }
        }
        lastTokenRequestTime = Instant.now();
        CjAccessTokenDataDto data = cjShoppingAuthClient.requestNewToken();
        storeTokenData(data);
        return cachedAccessToken;
    }

    private void storeTokenData(CjAccessTokenDataDto data) {
        this.cachedAccessToken = data.getAccessToken();
        this.cachedRefreshToken = data.getRefreshToken();
        this.accessTokenExpiry = parseToInstant(data.getAccessTokenExpiryDate());
        this.refreshTokenExpiry = parseToInstant(data.getRefreshTokenExpiryDate());
        saveToDatabase();
    }

    private void logTokenStatus() {
        long min = accessTokenExpiry != null ? ChronoUnit.MINUTES.between(Instant.now(), accessTokenExpiry) : -1;
        long refMin = refreshTokenExpiry != null ? ChronoUnit.MINUTES.between(Instant.now(), refreshTokenExpiry) : -1;
        log.info("  AccessToken  : {}...{} | expires: {} ({} min left)", maskStart(), maskEnd(), accessTokenExpiry,
                min);
        log.info("  RefreshToken : {}...{} | expires: {} ({} min left)", maskStart(cachedRefreshToken),
                maskEnd(cachedRefreshToken), refreshTokenExpiry, refMin);
    }

    private boolean isExpired() {
        return accessTokenExpiry == null || Instant.now().isAfter(accessTokenExpiry);
    }

    private boolean isAboutToExpire() {
        return accessTokenExpiry != null
                && Instant.now().plus(PROACTIVE_REFRESH_MINUTES, ChronoUnit.MINUTES).isAfter(accessTokenExpiry);
    }

    private boolean isRefreshTokenExpired() {
        return refreshTokenExpiry == null || Instant.now().isAfter(refreshTokenExpiry);
    }

    private Instant parseToInstant(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dateStr, CJ_DATE_FMT).toInstant(ZoneOffset.UTC);
            } catch (Exception e2) {
                log.warn("Could not parse CJ date '{}', treating as null", dateStr);
                return null;
            }
        }
    }

    private String maskStart() {
        return maskStart(cachedAccessToken);
    }

    private String maskEnd() {
        return maskEnd(cachedAccessToken);
    }

    private String maskStart(String token) {
        if (token == null || token.length() <= 8)
            return "****";
        return token.substring(0, 4);
    }

    private String maskEnd(String token) {
        if (token == null || token.length() <= 8)
            return "****";
        return token.substring(token.length() - 4);
    }
}
