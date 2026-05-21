package com.backandwhite.infrastructure.client.cj.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backandwhite.infrastructure.client.cj.dto.CjAccessTokenDataDto;
import com.backandwhite.infrastructure.db.postgres.entity.CjTokenEntity;
import com.backandwhite.infrastructure.db.postgres.repository.CjTokenJpaRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CjShoppingTokenManager")
class CjShoppingTokenManagerTest {

    private CjShoppingAuthClient authClient;
    private CjTokenJpaRepository repo;
    private CjShoppingTokenManager manager;

    @BeforeEach
    void setUp() {
        authClient = mock(CjShoppingAuthClient.class);
        repo = mock(CjTokenJpaRepository.class);
        manager = new CjShoppingTokenManager(authClient, repo);
    }

    private CjAccessTokenDataDto data(String access, String refresh, Instant accessExp, Instant refreshExp) {
        CjAccessTokenDataDto d = new CjAccessTokenDataDto();
        d.setAccessToken(access);
        d.setRefreshToken(refresh);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        if (accessExp != null) {
            d.setAccessTokenExpiryDate(fmt.format(accessExp.atZone(java.time.ZoneOffset.UTC)));
        }
        if (refreshExp != null) {
            d.setRefreshTokenExpiryDate(fmt.format(refreshExp.atZone(java.time.ZoneOffset.UTC)));
        }
        return d;
    }

    private void seedField(String fieldName, Object value) throws Exception {
        Field f = CjShoppingTokenManager.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(manager, value);
    }

    private static Instant inMinutes(long minutes) {
        return Instant.now().plus(minutes, ChronoUnit.MINUTES);
    }

    // ─── getValidAccessToken ──────────────────────────────────────────────

    @Test
    @DisplayName("getValidAccessToken: empty DB → requests new token and persists")
    void newToken_whenDbEmpty() {
        when(repo.findById("SINGLETON")).thenReturn(Optional.empty());
        when(authClient.requestNewToken()).thenReturn(data("AT1", "RT1", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT1");
        verify(authClient).requestNewToken();
        verify(repo).save(any(CjTokenEntity.class));
    }

    @Test
    @DisplayName("getValidAccessToken: cached valid token reused (no refresh)")
    void cachedValid_returnsCachedToken() throws Exception {
        // seed cached & loadedFromDb so no DB hit
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_OK");
        verify(authClient, never()).requestNewToken();
        verify(authClient, never()).refreshAccessToken(any());
    }

    @Test
    @DisplayName("getValidAccessToken: about-to-expire triggers proactive refresh")
    void aboutToExpire_proactiveRefresh() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OLD");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", inMinutes(10)); // <30 min
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK"))
                .thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_NEW");
        verify(authClient).refreshAccessToken("RT_OK");
    }

    @Test
    @DisplayName("getValidAccessToken: proactive refresh failure keeps current token")
    void aboutToExpire_proactiveRefreshFails_keepsCurrent() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OLD");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", inMinutes(10));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK")).thenThrow(new RuntimeException("network"));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_OLD");
    }

    @Test
    @DisplayName("getValidAccessToken: expired access + valid refresh → uses refresh")
    void expiredAccess_validRefresh() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_EXP");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", Instant.now().minus(10, ChronoUnit.MINUTES));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK"))
                .thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_NEW");
        verify(authClient).refreshAccessToken("RT_OK");
    }

    @Test
    @DisplayName("getValidAccessToken: expired access + refresh fails → falls back to new token")
    void expiredAccess_refreshFails_newToken() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_EXP");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", Instant.now().minus(10, ChronoUnit.MINUTES));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK")).thenThrow(new RuntimeException("auth401"));
        when(authClient.requestNewToken()).thenReturn(data("AT_FRESH", "RT_FRESH", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_FRESH");
        verify(authClient).refreshAccessToken("RT_OK");
        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("getValidAccessToken: expired refresh skips refresh, asks new token directly")
    void expiredRefresh_newToken() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_EXP");
        seedField("cachedRefreshToken", "RT_EXP");
        seedField("accessTokenExpiry", Instant.now().minus(10, ChronoUnit.MINUTES));
        seedField("refreshTokenExpiry", Instant.now().minus(60, ChronoUnit.MINUTES));

        when(authClient.requestNewToken()).thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_NEW");
        verify(authClient, never()).refreshAccessToken(any());
        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("getValidAccessToken: cooldown active and cached present → reuses cached")
    void cooldown_reusesCached() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_CACHED");
        seedField("accessTokenExpiry", Instant.now().minus(1, ChronoUnit.MINUTES)); // expired
        seedField("refreshTokenExpiry", null); // no refresh
        seedField("lastTokenRequestTime", Instant.now().minus(60, ChronoUnit.SECONDS)); // <300s

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_CACHED");
        verify(authClient, never()).requestNewToken();
    }

    @Test
    @DisplayName("getValidAccessToken: cooldown active and no cached token → throws")
    void cooldown_noCached_throws() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", null);
        seedField("lastTokenRequestTime", Instant.now().minus(60, ChronoUnit.SECONDS));

        assertThatThrownBy(() -> manager.getValidAccessToken()).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cooldown");
    }

    @Test
    @DisplayName("loadFromDatabase populates cached fields for first call")
    void loadFromDb_populatesCache() {
        CjTokenEntity entity = CjTokenEntity.builder().id("SINGLETON").accessToken("AT_DB").refreshToken("RT_DB")
                .accessTokenExpiry(inMinutes(120)).refreshTokenExpiry(inMinutes(60 * 24)).build();
        when(repo.findById("SINGLETON")).thenReturn(Optional.of(entity));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_DB");
        verify(repo).findById("SINGLETON");
        verify(authClient, never()).requestNewToken();
    }

    @Test
    @DisplayName("loadFromDatabase logs expired DB token and proceeds to renew")
    void loadFromDb_expiredToken_renews() {
        CjTokenEntity entity = CjTokenEntity.builder().id("SINGLETON").accessToken("AT_DB_EXP")
                .refreshToken("RT_DB_EXP").accessTokenExpiry(Instant.now().minus(120, ChronoUnit.MINUTES))
                .refreshTokenExpiry(Instant.now().minus(120, ChronoUnit.MINUTES)).build();
        when(repo.findById("SINGLETON")).thenReturn(Optional.of(entity));
        when(authClient.requestNewToken()).thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_NEW");
    }

    @Test
    @DisplayName("loadFromDatabase swallows DB exception and continues with new token")
    void loadFromDb_exception_continues() {
        when(repo.findById("SINGLETON")).thenThrow(new RuntimeException("db down"));
        when(authClient.requestNewToken()).thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT_NEW");
    }

    @Test
    @DisplayName("saveToDatabase swallows persistence errors")
    void saveToDb_exception_swallowed() {
        when(repo.findById("SINGLETON")).thenReturn(Optional.empty());
        when(authClient.requestNewToken()).thenReturn(data("AT_NEW", "RT_NEW", inMinutes(120), inMinutes(60 * 24)));
        when(repo.save(any(CjTokenEntity.class))).thenThrow(new RuntimeException("persist failure"));

        // Must not throw despite save failing
        String token = manager.getValidAccessToken();
        assertThat(token).isEqualTo("AT_NEW");
    }

    // ─── parseToInstant via storeTokenData ────────────────────────────────

    @ParameterizedTest(name = "parseToInstant: accessExp={0}, refreshExp={1} → token=AT")
    @CsvSource(value = {"2099-01-01T00:00:00+00:00,2099-12-31T00:00:00+00:00", "not-a-date,''",
            "NULL,NULL"}, nullValues = "NULL")
    @DisplayName("parseToInstant: ISO/garbage/null expiry strings all yield a usable token")
    void parseInstant_variants(String accessExp, String refreshExp) {
        when(repo.findById("SINGLETON")).thenReturn(Optional.empty());
        CjAccessTokenDataDto d = new CjAccessTokenDataDto();
        d.setAccessToken("AT");
        d.setRefreshToken("RT");
        d.setAccessTokenExpiryDate(accessExp);
        d.setRefreshTokenExpiryDate(refreshExp);
        when(authClient.requestNewToken()).thenReturn(d);

        String token = manager.getValidAccessToken();

        assertThat(token).isEqualTo("AT");
    }

    // ─── scheduledRefresh ─────────────────────────────────────────────────

    @Test
    @DisplayName("scheduledRefresh: empty DB → requests new token")
    void scheduled_empty_dbBootstrap() {
        when(repo.findById("SINGLETON")).thenReturn(Optional.empty());
        when(authClient.requestNewToken()).thenReturn(data("AT_BOOT", "RT_BOOT", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();

        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("scheduledRefresh: valid refresh token → calls refresh")
    void scheduled_refreshTokenValid_callsRefresh() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK"))
                .thenReturn(data("AT_R", "RT_R", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();

        verify(authClient).refreshAccessToken("RT_OK");
    }

    @Test
    @DisplayName("scheduledRefresh: refresh fails → falls back to new token request")
    void scheduled_refreshFails_newToken() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("RT_OK")).thenThrow(new RuntimeException("net"));
        when(authClient.requestNewToken()).thenReturn(data("AT_F", "RT_F", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();

        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("scheduledRefresh: expired refresh → directly requests new token")
    void scheduled_expiredRefresh() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("cachedRefreshToken", "RT_EXP");
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", Instant.now().minus(60, ChronoUnit.MINUTES));

        when(authClient.requestNewToken()).thenReturn(data("AT_F", "RT_F", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();

        verify(authClient).requestNewToken();
        verify(authClient, never()).refreshAccessToken(any());
    }

    @Test
    @DisplayName("scheduledRefresh: tokens with masking lengths >8 still log without throwing")
    void scheduled_logTokenStatus_longTokens() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "ACCESSTOKEN_LONG_VALUE_123");
        seedField("cachedRefreshToken", "REFRESHTOKEN_LONG_VALUE_456");
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.refreshAccessToken("REFRESHTOKEN_LONG_VALUE_456"))
                .thenReturn(data("AT", "RT", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();
        verify(authClient, atLeastOnce()).refreshAccessToken(any());
    }

    @Test
    @DisplayName("scheduledRefresh: null access expiry triggers logTokenStatus -1 branch")
    void scheduled_nullExpiry() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("cachedRefreshToken", "RT_OK");
        seedField("accessTokenExpiry", null); // null branch in logTokenStatus
        seedField("refreshTokenExpiry", null);

        when(authClient.requestNewToken()).thenReturn(data("AT", "RT", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();
        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("scheduledRefresh: null cached refresh token → directly requests new token")
    void scheduled_nullRefreshToken() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", "AT_OK");
        seedField("cachedRefreshToken", null); // refresh-null branch
        seedField("accessTokenExpiry", inMinutes(120));
        seedField("refreshTokenExpiry", inMinutes(60 * 24));

        when(authClient.requestNewToken()).thenReturn(data("AT", "RT", inMinutes(120), inMinutes(60 * 24)));

        manager.scheduledRefresh();
        verify(authClient).requestNewToken();
    }

    @Test
    @DisplayName("getValidAccessToken: cooldown elapsed (>5min) → request new token")
    void cooldownElapsed_newToken() throws Exception {
        seedField("loadedFromDb", true);
        seedField("cachedAccessToken", null);
        seedField("lastTokenRequestTime", Instant.now().minus(10, ChronoUnit.MINUTES));

        when(authClient.requestNewToken()).thenReturn(data("AT", "RT", inMinutes(120), inMinutes(60 * 24)));

        String token = manager.getValidAccessToken();
        assertThat(token).isEqualTo("AT");
    }

    @Test
    @DisplayName("loadFromDatabase: short access token (<=8 chars) triggers mask short-circuit")
    void loadFromDb_shortToken() {
        // forces mask functions to return "****"
        CjTokenEntity entity = CjTokenEntity.builder().id("SINGLETON").accessToken("ab").refreshToken("cd")
                .accessTokenExpiry(inMinutes(120)).refreshTokenExpiry(inMinutes(60 * 24)).build();
        when(repo.findById("SINGLETON")).thenReturn(Optional.of(entity));
        when(authClient.refreshAccessToken("cd"))
                .thenReturn(data("AT_LONG_VALUE", "RT_LONG_VALUE", inMinutes(120), inMinutes(60 * 24)));

        // Force scheduledRefresh path which hits logTokenStatus:
        assertThatCode(() -> manager.scheduledRefresh()).doesNotThrowAnyException();

        // No throw is enough (we covered both the short-token and the masking branches)
    }

    @Test
    @DisplayName("Repeated calls reuse cached token after first DB load (loadedFromDb guard)")
    void loadedFromDbGuard_skipsSecondLoad() {
        when(repo.findById("SINGLETON")).thenReturn(Optional.empty());
        when(authClient.requestNewToken()).thenReturn(data("AT", "RT", inMinutes(120), inMinutes(60 * 24)));

        manager.getValidAccessToken();
        manager.getValidAccessToken();

        verify(repo, times(1)).findById("SINGLETON");
    }
}
