/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.usecases;

import com.akademiaplus.config.MagicLinkProperties;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.magiclink.interfaceadapters.MagicLinkTokenRepository;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import com.akademiaplus.security.MagicLinkTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import openapi.akademiaplus.domain.security.dto.MagicLinkRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MagicLinkRequestUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MagicLinkRequestUseCase")
@ExtendWith(MockitoExtension.class)
class MagicLinkRequestUseCaseTest {

    private static final String TEST_EMAIL = "user@example.com";
    private static final Long TEST_TENANT_ID = 1L;
    private static final String TEST_TOKEN_HASH = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private static final String TEST_BASE_URL = "https://app.elatusdev.com";
    private static final String TEST_EMAIL_SUBJECT = "Sign in to ElatusDev";
    private static final int TEST_TOKEN_EXPIRY_MINUTES = 10;
    private static final int TEST_MAX_REQUESTS = 3;

    @Mock private MagicLinkTokenRepository magicLinkTokenRepository;
    @Mock private HashingService hashingService;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ApplicationContext applicationContext;
    @Mock private MagicLinkEmailSender emailSender;

    @Captor private ArgumentCaptor<MagicLinkTokenDataModel> tokenCaptor;
    @Captor private ArgumentCaptor<String> emailCaptor;
    @Captor private ArgumentCaptor<String> subjectCaptor;
    @Captor private ArgumentCaptor<String> htmlCaptor;

    private MagicLinkRequestUseCase useCase;
    private MagicLinkProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MagicLinkProperties(TEST_BASE_URL, TEST_TOKEN_EXPIRY_MINUTES, TEST_MAX_REQUESTS, TEST_EMAIL_SUBJECT);
        useCase = new MagicLinkRequestUseCase(
                magicLinkTokenRepository, hashingService, properties,
                tenantContextHolder, applicationContext);
        // Inject emailSender via reflection since it's @Autowired(required=false)
        try {
            var field = MagicLinkRequestUseCase.class.getDeclaredField("emailSender");
            field.setAccessible(true);
            field.set(useCase, emailSender);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private MagicLinkRequestDTO buildRequest() {
        MagicLinkRequestDTO dto = new MagicLinkRequestDTO(TEST_EMAIL, TEST_TENANT_ID);
        return dto;
    }

    private void stubTokenCreation() {
        when(hashingService.generateHash(argThat(token -> token != null && token.length() == 43)))
                .thenReturn(TEST_TOKEN_HASH);
        when(applicationContext.getBean(MagicLinkTokenDataModel.class)).thenReturn(new MagicLinkTokenDataModel());
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("Should store SHA-256 hash in database when requesting magic link")
        void shouldStoreSha256Hash_whenRequestingMagicLink() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(TEST_TOKEN_HASH);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }

        @Test
        @DisplayName("Should hash generated token with HashingService when storing")
        void shouldHashToken_whenStoring() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
            verify(hashingService, times(1)).generateHash(rawTokenCaptor.capture());
            assertThat(rawTokenCaptor.getValue()).hasSize(43);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }
    }

    @Nested
    @DisplayName("Token Storage")
    class TokenStorage {

        @Test
        @DisplayName("Should save token entity to repository when requesting magic link")
        void shouldSaveTokenEntity_whenRequestingMagicLink() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            MagicLinkTokenDataModel saved = tokenCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }

        @Test
        @DisplayName("Should set expiry from properties when storing token")
        void shouldSetExpiryFromProperties_whenStoringToken() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            Instant expectedExpiry = Instant.now().plusSeconds(TEST_TOKEN_EXPIRY_MINUTES * 60L);
            assertThat(tokenCaptor.getValue().getExpiresAt())
                    .isCloseTo(expectedExpiry, within(5, java.time.temporal.ChronoUnit.SECONDS));
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }
    }

    @Nested
    @DisplayName("Email Delivery")
    class EmailDelivery {

        @Test
        @DisplayName("Should send email with magic link when requesting magic link")
        void shouldSendEmail_whenRequestingMagicLink() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(emailSender, times(1)).send(emailCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());
            assertThat(emailCaptor.getValue()).isEqualTo(TEST_EMAIL);
            assertThat(subjectCaptor.getValue()).isEqualTo(TEST_EMAIL_SUBJECT);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }

        @Test
        @DisplayName("Should build correct magic link URL when sending email")
        void shouldBuildCorrectUrl_whenSendingEmail() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(emailSender, times(1)).send(emailCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());
            String html = htmlCaptor.getValue();
            assertThat(html).contains(TEST_BASE_URL + "/auth/magic-link?token=");
            assertThat(html).contains("&tenant=" + TEST_TENANT_ID);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimiting {

        @Mock private RateLimiterService rateLimiterService;

        @BeforeEach
        void setUpRateLimiter() {
            useCase = new MagicLinkRequestUseCase(
                    magicLinkTokenRepository, hashingService, properties,
                    tenantContextHolder, applicationContext);
            // Inject optional dependencies via reflection
            try {
                var emailField = MagicLinkRequestUseCase.class.getDeclaredField("emailSender");
                emailField.setAccessible(true);
                emailField.set(useCase, emailSender);
                var rateLimitField = MagicLinkRequestUseCase.class.getDeclaredField("rateLimiterService");
                rateLimitField.setAccessible(true);
                rateLimitField.set(useCase, rateLimiterService);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Should check rate limit when requesting magic link")
        void shouldCheckRateLimit_whenRequestingMagicLink() {
            // Given
            stubTokenCreation();
            when(rateLimiterService.checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS))
                    .thenReturn(new RateLimitResult(true, TEST_MAX_REQUESTS, 2, Instant.now().plusSeconds(3600).getEpochSecond()));
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            verify(rateLimiterService, times(1)).checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender, rateLimiterService);
        }

        @Test
        @DisplayName("Should not send email when rate limit exceeded")
        void shouldNotSendEmail_whenRateLimitExceeded() {
            // Given
            when(rateLimiterService.checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS))
                    .thenReturn(new RateLimitResult(false, TEST_MAX_REQUESTS, 0, Instant.now().plusSeconds(3600).getEpochSecond()));
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            assertThat(useCase).isNotNull();
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(rateLimiterService, times(1)).checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS);
            verifyNoInteractions(emailSender, magicLinkTokenRepository, hashingService, applicationContext);
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender, rateLimiterService);
        }

        @Test
        @DisplayName("Should still return silently when rate limit exceeded")
        void shouldReturnSilently_whenRateLimitExceeded() {
            // Given
            when(rateLimiterService.checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS))
                    .thenReturn(new RateLimitResult(false, TEST_MAX_REQUESTS, 0, Instant.now().plusSeconds(3600).getEpochSecond()));
            MagicLinkRequestDTO dto = buildRequest();

            // When / Then — no exception thrown
            useCase.requestMagicLink(dto);

            assertThat(useCase).isNotNull();
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(rateLimiterService, times(1)).checkRateLimit(
                    MagicLinkRequestUseCase.RATE_LIMIT_KEY_PREFIX + TEST_EMAIL,
                    TEST_MAX_REQUESTS,
                    MagicLinkRequestUseCase.RATE_LIMIT_WINDOW_MS);
            verifyNoInteractions(emailSender, magicLinkTokenRepository, hashingService, applicationContext);
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender, rateLimiterService);
        }
    }

    @Nested
    @DisplayName("Anti-Enumeration")
    class AntiEnumeration {

        @Test
        @DisplayName("Should always succeed silently when email does not exist")
        void shouldAlwaysSucceedSilently_whenEmailDoesNotExist() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then — no exception thrown, email still sent
            verify(emailSender, times(1)).send(emailCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());
            assertThat(emailCaptor.getValue()).isEqualTo(TEST_EMAIL);
            assertThat(subjectCaptor.getValue()).isEqualTo(TEST_EMAIL_SUBJECT);
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }
    }

    @Nested
    @DisplayName("Tenant Context")
    class TenantContext {

        @Test
        @DisplayName("Should set tenant ID when requesting magic link")
        void shouldSetTenantId_whenRequestingMagicLink() {
            // Given
            stubTokenCreation();
            MagicLinkRequestDTO dto = buildRequest();

            // When
            useCase.requestMagicLink(dto);

            // Then
            assertThat(useCase).isNotNull();
            verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            verify(hashingService, times(1)).generateHash(anyString());
            verify(applicationContext, times(1)).getBean(MagicLinkTokenDataModel.class);
            verify(magicLinkTokenRepository, times(1)).save(any(MagicLinkTokenDataModel.class));
            verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
            verifyNoMoreInteractions(magicLinkTokenRepository, hashingService, tenantContextHolder,
                    applicationContext, emailSender);
        }
    }
}
