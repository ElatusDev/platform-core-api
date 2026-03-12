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
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
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
import static org.mockito.Mockito.*;

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
    private AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;

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
                jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
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
            verify(refreshTokenRepository, times(1)).save(any(RefreshTokenDataModel.class));
            verify(akademiaPlusRedisSessionStore, times(1)).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
            verify(refreshTokenRepository, times(1)).save(any(RefreshTokenDataModel.class));
            verify(akademiaPlusRedisSessionStore, times(1)).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
            verify(refreshTokenRepository, times(1)).save(any(RefreshTokenDataModel.class));
            verify(akademiaPlusRedisSessionStore, times(1)).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
            verify(refreshTokenRepository, times(1)).save(captor.capture());
            RefreshTokenDataModel saved = captor.getValue();
            assertThat(saved.getTokenHash()).isEqualTo(NEW_REFRESH_TOKEN_HASH);
            assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            verify(akademiaPlusRedisSessionStore, times(1)).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
            assertThat(useCase).isNotNull();
            verify(akademiaPlusRedisSessionStore, times(1)).storeSession(
                    NEW_JTI, USERNAME, TENANT_ID, Duration.ofMillis(ACCESS_TOKEN_VALIDITY_MS));
            verify(refreshTokenRepository, times(1)).save(any(RefreshTokenDataModel.class));
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
                    .isInstanceOf(TokenReuseDetectedException.class)
                    .hasMessage(String.format(TokenReuseDetectedException.ERROR_MESSAGE, FAMILY_ID));

            verify(refreshTokenRepository, times(1)).revokeAllByFamilyId(
                    org.mockito.ArgumentMatchers.eq(FAMILY_ID),
                    org.mockito.ArgumentMatchers.isA(Instant.class));
            verify(akademiaPlusRedisSessionStore, times(1)).revokeAllSessionsForUser(USERNAME, TENANT_ID);
            verifyNoInteractions(applicationContext, newRefreshTokenClaims);
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
                    .isInstanceOf(TokenReuseDetectedException.class)
                    .hasMessage(String.format(TokenReuseDetectedException.ERROR_MESSAGE, FAMILY_ID));

            verify(akademiaPlusRedisSessionStore, times(1)).revokeAllSessionsForUser(USERNAME, TENANT_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByFamilyId(
                    org.mockito.ArgumentMatchers.eq(FAMILY_ID),
                    org.mockito.ArgumentMatchers.isA(Instant.class));
            verifyNoInteractions(applicationContext, newRefreshTokenClaims);
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
        }

        @Test
        @DisplayName("Should not create new tokens when reuse detected")
        void shouldNotCreateNewTokens_whenReuseDetected() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            existingToken.setRevokedAt(Instant.now().minusSeconds(60));
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(TokenReuseDetectedException.class)
                    .hasMessage(String.format(TokenReuseDetectedException.ERROR_MESSAGE, FAMILY_ID));

            verify(jwtTokenProvider, never()).createAccessToken(USERNAME, TENANT_ID, java.util.Map.of(JwtTokenProvider.USER_ID_CLAIM, USER_ID));
            verify(jwtTokenProvider, never()).createRefreshToken(USERNAME, TENANT_ID, FAMILY_ID);
            verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.isA(RefreshTokenDataModel.class));
            verifyNoInteractions(applicationContext, newRefreshTokenClaims);
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
            verifyNoInteractions(akademiaPlusRedisSessionStore, applicationContext, newRefreshTokenClaims);
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
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
            verifyNoInteractions(jwtTokenProvider, akademiaPlusRedisSessionStore, applicationContext, newRefreshTokenClaims);
            verifyNoMoreInteractions(jwtTokenProvider, refreshTokenRepository, akademiaPlusRedisSessionStore,
                    hashingService, applicationContext, newRefreshTokenClaims);
        }

        @Test
        @DisplayName("Should not create new tokens when token expired")
        void shouldNotCreateNewTokens_whenTokenExpired() {
            // Given
            RefreshTokenDataModel existingToken = buildActiveToken();
            existingToken.setExpiresAt(Instant.now().minusSeconds(3600));
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.of(existingToken));

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class)
                    .hasMessage(TokenRefreshUseCase.ERROR_REFRESH_TOKEN_EXPIRED);

            verify(jwtTokenProvider, never()).createAccessToken(USERNAME, TENANT_ID, java.util.Map.of(JwtTokenProvider.USER_ID_CLAIM, USER_ID));
            verify(jwtTokenProvider, never()).createRefreshToken(USERNAME, TENANT_ID, FAMILY_ID);
            verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.isA(RefreshTokenDataModel.class));
            verifyNoInteractions(akademiaPlusRedisSessionStore, applicationContext, newRefreshTokenClaims);
        }

        @Test
        @DisplayName("Should not create new tokens when token not found")
        void shouldNotCreateNewTokens_whenTokenNotFound() {
            // Given
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RefreshTokenExpiredException.class)
                    .hasMessage(TokenRefreshUseCase.ERROR_REFRESH_TOKEN_NOT_FOUND);

            verify(jwtTokenProvider, never()).createAccessToken(USERNAME, TENANT_ID, java.util.Map.of(JwtTokenProvider.USER_ID_CLAIM, USER_ID));
            verify(jwtTokenProvider, never()).createRefreshToken(USERNAME, TENANT_ID, FAMILY_ID);
            verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.isA(RefreshTokenDataModel.class));
            verifyNoInteractions(jwtTokenProvider, akademiaPlusRedisSessionStore, applicationContext, newRefreshTokenClaims);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate RuntimeException when hashingService throws")
        void shouldPropagateException_whenHashingServiceThrows() {
            // Given
            RuntimeException cause = new RuntimeException("hashing failed");
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hashing failed");

            verify(hashingService, times(1)).generateHash(CURRENT_REFRESH_TOKEN);
            verifyNoInteractions(refreshTokenRepository, jwtTokenProvider, akademiaPlusRedisSessionStore,
                    applicationContext, newRefreshTokenClaims);
        }

        @Test
        @DisplayName("Should propagate RuntimeException when refreshTokenRepository throws")
        void shouldPropagateException_whenRefreshTokenRepositoryThrows() {
            // Given
            RuntimeException cause = new RuntimeException("db error");
            when(hashingService.generateHash(CURRENT_REFRESH_TOKEN)).thenReturn(CURRENT_TOKEN_HASH);
            when(refreshTokenRepository.findByTokenHash(CURRENT_TOKEN_HASH)).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> useCase.refresh(CURRENT_REFRESH_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db error");

            verify(hashingService, times(1)).generateHash(CURRENT_REFRESH_TOKEN);
            verify(refreshTokenRepository, times(1)).findByTokenHash(CURRENT_TOKEN_HASH);
            verifyNoInteractions(jwtTokenProvider, akademiaPlusRedisSessionStore,
                    applicationContext, newRefreshTokenClaims);
        }
    }
}
