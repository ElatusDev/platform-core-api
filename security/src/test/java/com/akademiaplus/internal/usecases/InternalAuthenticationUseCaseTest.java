/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.exceptions.InvalidLoginException;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.security.InternalAuthDataModel;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("InternalAuthenticationUseCase")
@ExtendWith(MockitoExtension.class)
class InternalAuthenticationUseCaseTest {

    private static final String USERNAME = "teacher@akademia.com";
    private static final String USERNAME_HASH = "hash-teacher";
    private static final String PASSWORD = "password123";
    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 1L;
    private static final String ROLE = "COLLABORATOR";
    private static final String ACCESS_TOKEN = "access-token-jwt";
    private static final String REFRESH_TOKEN = "refresh-token-jwt";
    private static final String JTI = "jti-123";
    private static final long ACCESS_TOKEN_VALIDITY_MS = 3600000L;

    @Mock private InternalAuthRepository repository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private HashingService hashingService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;
    @Mock private ApplicationContext applicationContext;

    private InternalAuthDataModel buildAuth() {
        InternalAuthDataModel auth = new InternalAuthDataModel();
        auth.setInternalAuthId(USER_ID);
        auth.setUsername(USERNAME);
        auth.setUsernameHash(USERNAME_HASH);
        auth.setPassword(PASSWORD);
        auth.setRole(ROLE);
        auth.setTenantId(TENANT_ID);
        return auth;
    }

    private void stubTokenCreation() {
        when(jwtTokenProvider.createAccessToken(eq(USERNAME), eq(TENANT_ID), anyMap()))
                .thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.getJti(ACCESS_TOKEN)).thenReturn(JTI);
        when(jwtTokenProvider.getAccessTokenValidityInMs()).thenReturn(ACCESS_TOKEN_VALIDITY_MS);
        when(jwtTokenProvider.createRefreshToken(eq(USERNAME), eq(TENANT_ID), anyString()))
                .thenReturn(REFRESH_TOKEN);

        Claims refreshClaims = mock(Claims.class);
        when(refreshClaims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_MS));
        when(jwtTokenProvider.getClaims(REFRESH_TOKEN)).thenReturn(refreshClaims);

        when(hashingService.generateHash(REFRESH_TOKEN)).thenReturn("refresh-hash");

        RefreshTokenDataModel refreshTokenEntity = new RefreshTokenDataModel();
        when(applicationContext.getBean(RefreshTokenDataModel.class)).thenReturn(refreshTokenEntity);
    }

    @Nested
    @DisplayName("Profile Claim Enrichment")
    class ProfileClaimEnrichment {

        @Test
        @DisplayName("Should invoke enrichers on successful login and include enriched claims")
        void shouldInvokeEnrichers_whenLoginSucceeds() {
            // Given
            ProfileClaimEnricher enricher = mock(ProfileClaimEnricher.class);
            doAnswer(invocation -> {
                Map<String, Object> claims = invocation.getArgument(1);
                claims.put(JwtTokenProvider.PROFILE_TYPE_CLAIM, "COLLABORATOR");
                claims.put(JwtTokenProvider.PROFILE_ID_CLAIM, 999L);
                return null;
            }).when(enricher).enrich(eq(USER_ID), anyMap());

            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of(enricher));

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(repository.findByUsernameHash(USERNAME_HASH)).thenReturn(Optional.of(buildAuth()));
            stubTokenCreation();

            // When
            LoginResult result = useCase.login(USERNAME, PASSWORD);

            // Then — state
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.username()).isEqualTo(USERNAME);

            // Then — enricher was invoked
            verify(enricher, times(1)).enrich(eq(USER_ID), anyMap());

            // Then — verify claims passed to token creation include enriched values
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtTokenProvider, times(1)).createAccessToken(eq(USERNAME), eq(TENANT_ID), claimsCaptor.capture());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertThat(capturedClaims).containsEntry(JwtTokenProvider.PROFILE_TYPE_CLAIM, "COLLABORATOR");
            assertThat(capturedClaims).containsEntry(JwtTokenProvider.PROFILE_ID_CLAIM, 999L);
        }

        @Test
        @DisplayName("Should not add profile claims when enricher does not modify claims (employee login)")
        void shouldNotAddProfileClaims_whenEnricherDoesNotModifyClaims() {
            // Given — enricher that does nothing (simulates employee with no collaborator record)
            ProfileClaimEnricher enricher = mock(ProfileClaimEnricher.class);

            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of(enricher));

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(repository.findByUsernameHash(USERNAME_HASH)).thenReturn(Optional.of(buildAuth()));
            stubTokenCreation();

            // When
            LoginResult result = useCase.login(USERNAME, PASSWORD);

            // Then — state
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);

            // Then — enricher was invoked but claims should not have profile entries
            verify(enricher, times(1)).enrich(eq(USER_ID), anyMap());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtTokenProvider, times(1)).createAccessToken(eq(USERNAME), eq(TENANT_ID), claimsCaptor.capture());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertThat(capturedClaims).doesNotContainKey(JwtTokenProvider.PROFILE_TYPE_CLAIM);
            assertThat(capturedClaims).doesNotContainKey(JwtTokenProvider.PROFILE_ID_CLAIM);
        }

        @Test
        @DisplayName("Should work correctly with no enrichers (empty list)")
        void shouldWorkCorrectly_whenNoEnrichersConfigured() {
            // Given
            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of());

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(repository.findByUsernameHash(USERNAME_HASH)).thenReturn(Optional.of(buildAuth()));
            stubTokenCreation();

            // When
            LoginResult result = useCase.login(USERNAME, PASSWORD);

            // Then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.username()).isEqualTo(USERNAME);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtTokenProvider, times(1)).createAccessToken(eq(USERNAME), eq(TENANT_ID), claimsCaptor.capture());

            Map<String, Object> capturedClaims = claimsCaptor.getValue();
            assertThat(capturedClaims).doesNotContainKey(JwtTokenProvider.PROFILE_TYPE_CLAIM);
            assertThat(capturedClaims).doesNotContainKey(JwtTokenProvider.PROFILE_ID_CLAIM);
        }
    }

    @Nested
    @DisplayName("Authentication")
    class Authentication {

        @Test
        @DisplayName("Should throw InvalidLoginException when credentials are invalid")
        void shouldThrowInvalidLoginException_whenCredentialsInvalid() {
            // Given
            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of());

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(repository.findByUsernameHash(USERNAME_HASH)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.login(USERNAME, PASSWORD))
                    .isInstanceOf(InvalidLoginException.class)
                    .hasMessage(null);

            // Then — interactions
            verify(hashingService, times(1)).generateHash(USERNAME);
            verify(repository, times(1)).findByUsernameHash(USERNAME_HASH);
            verifyNoInteractions(jwtTokenProvider, refreshTokenRepository,
                    akademiaPlusRedisSessionStore, applicationContext);
        }

        @Test
        @DisplayName("Should throw InvalidLoginException when password does not match")
        void shouldThrowInvalidLoginException_whenPasswordDoesNotMatch() {
            // Given
            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of());

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            InternalAuthDataModel auth = buildAuth();
            when(repository.findByUsernameHash(USERNAME_HASH)).thenReturn(Optional.of(auth));

            // When / Then
            assertThatThrownBy(() -> useCase.login(USERNAME, "wrong-password"))
                    .isInstanceOf(InvalidLoginException.class)
                    .hasMessage(null);

            // Then — interactions
            verify(repository, times(1)).findByUsernameHash(USERNAME_HASH);
            verifyNoInteractions(jwtTokenProvider, refreshTokenRepository,
                    akademiaPlusRedisSessionStore, applicationContext);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when hashingService throws")
        void shouldPropagateException_whenHashingServiceThrows() {
            // Given
            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of());

            RuntimeException cause = new RuntimeException("hash failure");
            when(hashingService.generateHash(USERNAME)).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> useCase.login(USERNAME, PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hash failure");

            verify(hashingService, times(1)).generateHash(USERNAME);
            verifyNoInteractions(repository, jwtTokenProvider, refreshTokenRepository,
                    akademiaPlusRedisSessionStore, applicationContext);
        }

        @Test
        @DisplayName("Should propagate exception when repository throws")
        void shouldPropagateException_whenRepositoryThrows() {
            // Given
            InternalAuthenticationUseCase useCase = new InternalAuthenticationUseCase(
                    repository, jwtTokenProvider, hashingService,
                    refreshTokenRepository, akademiaPlusRedisSessionStore,
                    applicationContext, List.of());

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            RuntimeException cause = new RuntimeException("db error");
            when(repository.findByUsernameHash(USERNAME_HASH)).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> useCase.login(USERNAME, PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db error");

            verify(hashingService, times(1)).generateHash(USERNAME);
            verify(repository, times(1)).findByUsernameHash(USERNAME_HASH);
            verifyNoInteractions(jwtTokenProvider, refreshTokenRepository,
                    akademiaPlusRedisSessionStore, applicationContext);
        }
    }
}
