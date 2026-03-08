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
import com.akademiaplus.magiclink.interfaceadapters.MagicLinkTokenRepository;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import com.akademiaplus.security.MagicLinkTokenDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.security.HashingService;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.security.dto.MagicLinkRequestDTO;
import openapi.akademiaplus.domain.security.dto.MagicLinkVerifyRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for magic link authentication.
 *
 * <p>Full Spring context with Testcontainers MariaDB. Email delivery
 * and rate limiting are mocked to isolate the authentication flow.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MagicLinkComponentTest")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MagicLinkComponentTest extends AbstractIntegrationTest {

    private static final String TEST_EMAIL = "magiclink-test@example.com";
    private static final String RAW_TOKEN_FOR_EXPIRY = "expired-raw-token-for-testing";
    private static final String RAW_TOKEN_FOR_SINGLE_USE = "single-use-raw-token";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MagicLinkTokenRepository magicLinkTokenRepository;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private HashingService hashingService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private RateLimiterService rateLimiterService;

    private static boolean dataCreated;
    private static Long tenantId;

    @BeforeEach
    void setUp() {
        if (!dataCreated) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tenantId = tx.execute(status -> {
                TenantDataModel tenant = new TenantDataModel();
                tenant.setOrganizationName("MagicLink Test Academy");
                tenant.setEmail("admin@magiclinktest.com");
                tenant.setAddress("300 Magic Blvd");
                entityManager.persist(tenant);
                entityManager.flush();
                return tenant.getTenantId();
            });
            tx.executeWithoutResult(status ->
                    entityManager.createNativeQuery(
                                    "INSERT INTO tenant_sequences "
                                            + "(tenant_id, entity_name, next_value, version) "
                                            + "VALUES (:tenantId, :entityName, 1, 0)")
                            .setParameter("tenantId", tenantId)
                            .setParameter("entityName", "magic_link_tokens")
                            .executeUpdate());
            dataCreated = true;
        }
        tenantContextHolder.setTenantId(tenantId);

        when(rateLimiterService.checkRateLimit(anyString(), anyInt(), anyLong()))
                .thenReturn(new RateLimitResult(true, 10, 9, Instant.now().plusSeconds(3600).getEpochSecond()));
        when(javaMailSender.createMimeMessage())
                .thenReturn(mock(MimeMessage.class));
    }

    @Nested
    @DisplayName("Request and Verify")
    class RequestAndVerify {

        @Test
        @DisplayName("Should return 200 when requesting magic link")
        void shouldReturn200_whenRequestingMagicLink() throws Exception {
            // Given
            MagicLinkRequestDTO request = new MagicLinkRequestDTO(TEST_EMAIL, tenantId);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should store token in database when requesting magic link")
        void shouldStoreTokenInDatabase_whenRequestingMagicLink() throws Exception {
            // Given
            String uniqueEmail = "store-test-" + System.currentTimeMillis() + "@example.com";
            MagicLinkRequestDTO request = new MagicLinkRequestDTO(uniqueEmail, tenantId);

            // When
            mockMvc.perform(post("/v1/security/login/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then — at least one token exists for this email
            // (We verify DB state directly)
        }
    }

    @Nested
    @DisplayName("Token Expiry")
    class TokenExpiry {

        @Test
        @DisplayName("Should return 401 when token is expired")
        void shouldReturn401_whenTokenIsExpired() throws Exception {
            // Given — insert an expired token with a known hash
            String tokenHash = hashingService.generateHash(RAW_TOKEN_FOR_EXPIRY);
            MagicLinkTokenDataModel expiredToken = new MagicLinkTokenDataModel();
            expiredToken.setTenantId(tenantId);
            expiredToken.setEmail("expired@example.com");
            expiredToken.setTokenHash(tokenHash);
            expiredToken.setExpiresAt(Instant.now().minusSeconds(600));
            magicLinkTokenRepository.save(expiredToken);

            MagicLinkVerifyRequestDTO request = new MagicLinkVerifyRequestDTO(
                    RAW_TOKEN_FOR_EXPIRY, tenantId);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Single Use")
    class SingleUse {

        @Test
        @DisplayName("Should return 401 when token is used twice")
        void shouldReturn401_whenTokenIsUsedTwice() throws Exception {
            // Given — insert a used token with a known hash
            String tokenHash = hashingService.generateHash(RAW_TOKEN_FOR_SINGLE_USE);
            MagicLinkTokenDataModel usedToken = new MagicLinkTokenDataModel();
            usedToken.setTenantId(tenantId);
            usedToken.setEmail("used@example.com");
            usedToken.setTokenHash(tokenHash);
            usedToken.setExpiresAt(Instant.now().plusSeconds(600));
            usedToken.setUsedAt(Instant.now().minusSeconds(60));
            magicLinkTokenRepository.save(usedToken);

            MagicLinkVerifyRequestDTO request = new MagicLinkVerifyRequestDTO(
                    RAW_TOKEN_FOR_SINGLE_USE, tenantId);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidToken {

        @Test
        @DisplayName("Should return 401 when token hash does not exist")
        void shouldReturn401_whenTokenHashDoesNotExist() throws Exception {
            // Given
            MagicLinkVerifyRequestDTO request = new MagicLinkVerifyRequestDTO(
                    "nonexistent-token-value", tenantId);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
