/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import com.akademiaplus.security.RefreshTokenDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.security.HashingService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for token refresh endpoint.
 *
 * <p>Full Spring context with Testcontainers MariaDB. Redis is mocked
 * since these tests focus on the refresh token rotation logic (DB-backed),
 * not the session store.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("TokenRefreshComponentTest")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TokenRefreshComponentTest extends AbstractIntegrationTest {

    public static final String REFRESH_PATH = "/v1/security/token/refresh";
    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String TEST_USERNAME = "token-refresh-test-user";
    public static final Long TEST_USER_ID = 100L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private HashingService hashingService;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;

    private static boolean dataCreated;
    private static Long tenantId;
    private String familyId;

    @BeforeEach
    void setUp() {
        if (!dataCreated) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tenantId = createTenant(tx);
            createTenantSequences(tx);
            dataCreated = true;
        }
        tenantContextHolder.setTenantId(tenantId);
        familyId = UUID.randomUUID().toString();
    }

    @Nested
    @DisplayName("Valid Refresh")
    class ValidRefresh {

        @Test
        @DisplayName("Should return 200 with new token cookies when refresh token is valid")
        void shouldReturn200_withNewTokenCookies_whenRefreshTokenIsValid() throws Exception {
            // Given — create a real refresh token and store its hash in DB
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USERNAME, tenantId, familyId);
            String tokenHash = hashingService.generateHash(refreshToken);
            persistRefreshToken(tokenHash, familyId, Instant.now().plusSeconds(86400));

            Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
            refreshCookie.setPath("/api/v1/security/token");

            // When / Then
            mockMvc.perform(post(REFRESH_PATH)
                            .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(SET_COOKIE_HEADER));
        }
    }

    @Nested
    @DisplayName("Expired Refresh Token")
    class ExpiredRefreshToken {

        @Test
        @DisplayName("Should return 401 when refresh token is expired")
        void shouldReturn401_whenRefreshTokenIsExpired() throws Exception {
            // Given — create a refresh token and store with past expiration
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USERNAME, tenantId, familyId);
            String tokenHash = hashingService.generateHash(refreshToken);
            persistRefreshToken(tokenHash, familyId, Instant.now().minusSeconds(600));

            Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);

            // When / Then
            mockMvc.perform(post(REFRESH_PATH)
                            .cookie(refreshCookie))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED"));
        }
    }

    @Nested
    @DisplayName("Reused (Rotated) Refresh Token")
    class ReusedRefreshToken {

        @Test
        @DisplayName("Should return 401 and revoke family when reused token is detected")
        void shouldReturn401_andRevokeFamily_whenReusedTokenDetected() throws Exception {
            // Given — create a refresh token that was already consumed (revokedAt set)
            String refreshToken = jwtTokenProvider.createRefreshToken(TEST_USERNAME, tenantId, familyId);
            String tokenHash = hashingService.generateHash(refreshToken);
            RefreshTokenDataModel token = persistRefreshToken(
                    tokenHash, familyId, Instant.now().plusSeconds(86400));
            token.setRevokedAt(Instant.now().minusSeconds(60));
            refreshTokenRepository.save(token);

            Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);

            // When / Then
            mockMvc.perform(post(REFRESH_PATH)
                            .cookie(refreshCookie))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_REUSE_DETECTED"));
        }
    }

    @Nested
    @DisplayName("Missing Refresh Token")
    class MissingRefreshToken {

        @Test
        @DisplayName("Should return 401 when no refresh token cookie is present")
        void shouldReturn401_whenNoRefreshTokenCookiePresent() throws Exception {
            // Given — no cookie sent

            // When / Then
            mockMvc.perform(post(REFRESH_PATH))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED"));
        }
    }

    private RefreshTokenDataModel persistRefreshToken(String tokenHash, String familyId, Instant expiresAt) {
        RefreshTokenDataModel token = new RefreshTokenDataModel();
        token.setTenantId(tenantId);
        token.setTokenHash(tokenHash);
        token.setFamilyId(familyId);
        token.setUserId(TEST_USER_ID);
        token.setUsername(TEST_USERNAME);
        token.setExpiresAt(expiresAt);
        return refreshTokenRepository.save(token);
    }

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName("TokenRefresh Test Academy");
            tenant.setEmail("admin@tokenrefreshtest.com");
            tenant.setAddress("100 Test Blvd");
            entityManager.persist(tenant);
            entityManager.flush();
            return tenant.getTenantId();
        });
    }

    private void createTenantSequences(TransactionTemplate tx) {
        tx.executeWithoutResult(status ->
                entityManager.createNativeQuery(
                                "INSERT INTO tenant_sequences "
                                        + "(tenant_id, entity_name, next_value, version) "
                                        + "VALUES (:tenantId, :entityName, 1, 0)")
                        .setParameter("tenantId", tenantId)
                        .setParameter("entityName", "refresh_tokens")
                        .executeUpdate());
    }
}
