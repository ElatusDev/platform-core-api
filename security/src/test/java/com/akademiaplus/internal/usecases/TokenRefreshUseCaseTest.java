/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.internal.exceptions.RefreshTokenExpiredException;
import com.akademiaplus.internal.exceptions.TokenReuseDetectedException;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import com.akademiaplus.internal.usecases.domain.TokenRefreshResult;
import com.akademiaplus.security.RefreshTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenRefreshUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("TokenRefreshUseCase")
@ExtendWith(MockitoExtension.class)
class TokenRefreshUseCaseTest {

    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token";
    private static final String CURRENT_TOKEN_HASH = "current-token-hash";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String NEW_REFRESH_TOKEN_HASH = "new-refresh-token-hash";
    private static final String NEW_JTI = "new-jti-uuid";
    private static final String USERNAME = "testuser";
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;
    private static final String FAMILY_ID = "family-uuid";
    private static final long ACCESS_TOKEN_VALIDITY_MS = 900_000L;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedisSessionStore redisSessionStore;

    @Mock
    private HashingService hashingService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Claims newRefreshTokenClaims;

    private TokenRefreshUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TokenRefreshUseCase(
                jwtTokenProvider, refreshTokenRepository, redisSessionStore,
                hashingService, applicationContext);
    }

    private RefreshTokenDataModel buildActiveToken() {
        RefreshTokenDataModel token = new RefreshTokenDataModel();
        token.setTenantId(TENANT_ID);
        token.setRefreshTokenId(1L);
        token.setTokenHash(CURRENT_TOKEN_HASH);
        token.setFamilyId(FAMILY_ID);
        token.setUserId(USER_ID);
        token.setUsername(USERNAME);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevokedAt(null);
        return token;
    }

    private void stubNewTokenCreation() {
        when(jwtTokenProvider.createAccessToken(USERNAME, TENANT_ID, java.util.Map.of(JwtTokenProvider.USER_ID_CLAIM, USER_ID)))
                .thenReturn(NEW_ACCESS_TOKEN);
        when(jwtTokenProvider.getJti(NEW_ACCESS_TOKEN)).thenReturn(NEW_JTI);
        when(jwtTokenProvider.getAccessTokenValidityInMs()).thenReturn(ACCESS_TOKEN_VALIDITY_MS);
        when(jwtTokenProvider.createRefreshToken(USERNAME, TENANT_ID, FAMILY_ID)).thenReturn(NEW_REFRESH_TOKEN);
        when(hashingService.generateHash(NEW_REFRESH_TOKEN)).thenReturn(NEW_REFRESH_TOKEN_HASH);
        when(jwtTokenProvider.getClaims(NEW_REFRESH_TOKEN)).thenReturn(newRefreshTokenClaims);
        when(newRefreshTokenClaims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 2592000000L));

        RefreshTokenDataModel newTokenEntity = new RefreshTokenDataModel();
        when(applicationContext.getBean(RefreshTokenDataModel.class)).thenReturn(newTokenEntity);
    }

    @Nested
    @DisplayName("Successful Rotation")
    class SuccessfulRotation {

        @Test
        @DisplayName("Should issue new access token when refresh token is valid")
        void shouldIssueNewAccessToken_whenRefreshTokenIsValid() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));
            stubNewTokenCreation();

            // When
            TokenRefreshResult result = useCase.refresh(CURRENT_REFRESH_TOKEN);

            // Then
            assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should issue new refresh token when refresh token is valid")
        void shouldIssueNewRefreshToken_whenRefreshTokenIsValid() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));
            stubNewTokenCreation();

            // When
            TokenRefreshResult result = useCase.refresh(CURRENT_REFRESH_TOKEN);

            // Then
            assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Should mark old token as consumed when rotating")
        void shouldMarkOldTokenAsConsumed_whenRotating() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));
            stubNewTokenCreation();

            // When
            useCase.refresh(CURRENT_REFRESH_TOKEN);

            // Then
            assertThat(existingToken.getRevokedAt()).isNotNull();
            assertThat(existingToken.getReplacedByTokenHash()).isEqualTo(NEW_REFRESH_TOKEN_HASH);
        }

        @Test
        @DisplayName("Should create new refresh token entity when rotating")
        void shouldCreateNewRefreshTokenEntity_whenRotating() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));
            stubNewTokenCreation();

            // When
            useCase.refresh(CURRENT_REFRESH_TOKEN);

            // Then
            ArgumentCaptor<RefreshTokenDataModel> captor = ArgumentCaptor.forClass(RefreshTokenDataModel.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshTokenDataModel saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo(NEW_REFRESH_TOKEN_HASH);
            assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should store new session in Redis when rotating")
        void shouldStoreNewSessionInRedis_whenRotating() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));
            stubNewTokenCreation();

            // When
            useCase.refresh(CURRENT_REFRESH_TOKEN);

            // Then
            verify(redisSessionStore).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
        }
    }

    @Nested
    @DisplayName("Reuse Detection")
    class ReuseDetection {

        @Test
        @DisplayName("Should revoke all family tokens when consumed token is reused")
        void shouldRevokeAllFamilyTokens_whenConsumedTokenReused() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            existingToken.setRevokedAt(Instant.now().minusSeconds(60));
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(TokenReuseDetectedException.class);

            verify(refreshTokenRepository).revokeAllByFamilyId(
                    org.mockito.ArgumentMatchers.eq(FAMILY_ID),
                    org.mockito.ArgumentMatchers.isA(Instant.class));
        }

        @Test
        @DisplayName("Should revoke all Redis sessions for user when reuse detected")
        void shouldRevokeAllRedisSessionsForUser_whenReuseDetected() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            existingToken.setRevokedAt(Instant.now().minusSeconds(60));
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(TokenReuseDetectedException.class);

            verify(redisSessionStore).revokeAllSessionsForUser(USERNAME, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Expired Token")
    class ExpiredToken {

        @Test
        @DisplayName("Should throw RefreshTokenExpiredException when token has expired")
        void shouldThrowRefreshTokenExpiredException_whenTokenExpired() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            existingToken.setExpiresAt(Instant.now().minusSeconds(3600));
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class)
                    .hasMessage(TokenRefreshUseCase.ERROR_REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("Should throw RefreshTokenExpiredException when token not found")
        void shouldThrowRefreshTokenExpiredException_whenTokenNotFound() {
            // Given
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class)
                    .hasMessage(TokenRefreshUseCase.ERROR_REFRESH_TOKEN_NOT_FOUND);
        }
    }
}
