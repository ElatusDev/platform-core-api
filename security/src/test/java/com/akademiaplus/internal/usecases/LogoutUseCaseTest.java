/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogoutUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("LogoutUseCase")
@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String USERNAME = "testuser";
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedisSessionStore redisSessionStore;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Claims claims;

    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(refreshTokenRepository, redisSessionStore, jwtTokenProvider);
    }

    @Nested
    @DisplayName("Successful Logout")
    class SuccessfulLogout {

        @Test
        @DisplayName("Should revoke all refresh tokens for user when logging out")
        void shouldRevokeAllRefreshTokens_whenLoggingOut() {
            // Given
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(claims.get(JwtTokenProvider.USER_ID_CLAIM, Long.class)).thenReturn(USER_ID);

            // When
            useCase.logout(ACCESS_TOKEN);

            // Then
            verify(refreshTokenRepository).revokeAllByUserIdAndTenantId(
                    org.mockito.ArgumentMatchers.eq(USER_ID),
                    org.mockito.ArgumentMatchers.eq(TENANT_ID),
                    org.mockito.ArgumentMatchers.isA(Instant.class));
        }

        @Test
        @DisplayName("Should revoke all Redis sessions for user when logging out")
        void shouldRevokeAllRedisSessions_whenLoggingOut() {
            // Given
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(claims.get(JwtTokenProvider.USER_ID_CLAIM, Long.class)).thenReturn(USER_ID);

            // When
            useCase.logout(ACCESS_TOKEN);

            // Then
            verify(redisSessionStore).revokeAllSessionsForUser(USERNAME, TENANT_ID);
        }
    }
}
