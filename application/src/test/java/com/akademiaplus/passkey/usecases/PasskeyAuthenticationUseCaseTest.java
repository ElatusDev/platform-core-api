/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.usecases;

import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import com.akademiaplus.passkey.exceptions.PasskeyAuthenticationException;
import com.akademiaplus.passkey.interfaceadapters.PasskeyCredentialJpaRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasskeyAuthenticationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyAuthenticationUseCase")
@ExtendWith(MockitoExtension.class)
class PasskeyAuthenticationUseCaseTest {

    private static final String USERNAME = "testuser@example.com";
    private static final String USERNAME_HASH = "hash-of-username";
    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 1L;
    private static final String DISPLAY_NAME = "My Passkey";
    private static final String CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2U";
    private static final byte[] USER_HANDLE_BYTES = new byte[]{100, 101, 102, 103};
    private static final String OPTIONS_JSON = "{\"challenge\":\"test\"}";
    private static final String CREDENTIALS_GET_JSON = "{\"publicKey\":{}}";

    @Mock private RelyingParty relyingParty;
    @Mock private PasskeyChallengeStore challengeStore;
    @Mock private PasskeyCredentialJpaRepository credentialRepository;
    @Mock private PasskeyRegistrationUseCase registrationUseCase;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private InternalAuthRepository internalAuthRepository;
    @Mock private HashingService hashingService;
    @Mock private RedisSessionStore redisSessionStore;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ApplicationContext applicationContext;

    private PasskeyAuthenticationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PasskeyAuthenticationUseCase(
                relyingParty, challengeStore, credentialRepository, registrationUseCase,
                jwtTokenProvider, internalAuthRepository, hashingService,
                redisSessionStore, refreshTokenRepository, applicationContext);
    }

    private InternalAuthDataModel createAuthDataModel() {
        InternalAuthDataModel auth = new InternalAuthDataModel();
        auth.setInternalAuthId(USER_ID);
        auth.setUsername(USERNAME);
        auth.setUsernameHash(USERNAME_HASH);
        auth.setRole("ADMIN");
        return auth;
    }

    private PasskeyCredentialDataModel createCredential() {
        PasskeyCredentialDataModel credential = new PasskeyCredentialDataModel();
        credential.setUserId(USER_ID);
        credential.setUserHandle(USER_HANDLE_BYTES);
        credential.setSignCount(5L);
        return credential;
    }

    @Nested
    @DisplayName("Registration Orchestration")
    class RegistrationOrchestration {

        @Test
        @DisplayName("Should delegate to registration use case when generating registration options")
        void shouldDelegateToRegistrationUseCase_whenGeneratingRegistrationOptions() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(credentialRepository.findByUserId(USER_ID)).thenReturn(List.of(createCredential()));
            when(registrationUseCase.generateRegistrationOptions(
                    eq(USER_ID), eq(USERNAME), eq(USER_HANDLE_BYTES), eq(TENANT_ID)))
                    .thenReturn(OPTIONS_JSON);

            // When
            String result = useCase.generateRegistrationOptions(USERNAME, TENANT_ID);

            // Then
            assertThat(result).isEqualTo(OPTIONS_JSON);
            verify(registrationUseCase).generateRegistrationOptions(
                    USER_ID, USERNAME, USER_HANDLE_BYTES, TENANT_ID);
        }

        @Test
        @DisplayName("Should generate new user handle when user has no existing credentials")
        void shouldGenerateNewUserHandle_whenUserHasNoExistingCredentials() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(credentialRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            ArgumentCaptor<byte[]> userHandleCaptor = ArgumentCaptor.forClass(byte[].class);
            when(registrationUseCase.generateRegistrationOptions(
                    eq(USER_ID), eq(USERNAME), userHandleCaptor.capture(), eq(TENANT_ID)))
                    .thenReturn(OPTIONS_JSON);

            // When
            useCase.generateRegistrationOptions(USERNAME, TENANT_ID);

            // Then
            byte[] generatedHandle = userHandleCaptor.getValue();
            assertThat(generatedHandle).hasSize(64);
        }

        @Test
        @DisplayName("Should throw PasskeyAuthenticationException when user is not found")
        void shouldThrowPasskeyAuthenticationException_whenUserNotFound() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.generateRegistrationOptions(USERNAME, TENANT_ID))
                    .isInstanceOf(PasskeyAuthenticationException.class)
                    .hasMessageContaining(PasskeyAuthenticationUseCase.ERROR_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should delegate to registration use case when completing registration")
        void shouldDelegateToRegistrationUseCase_whenCompletingRegistration() {
            // Given
            String responseJson = "{\"response\":{}}";
            when(registrationUseCase.completeRegistration(responseJson, TENANT_ID, DISPLAY_NAME))
                    .thenReturn(DISPLAY_NAME);

            // When
            String result = useCase.completeRegistration(responseJson, TENANT_ID, DISPLAY_NAME);

            // Then
            assertThat(result).isEqualTo(DISPLAY_NAME);
            verify(registrationUseCase).completeRegistration(responseJson, TENANT_ID, DISPLAY_NAME);
        }
    }

    @Nested
    @DisplayName("Login Options Generation")
    class LoginOptionsGeneration {

        @Test
        @DisplayName("Should store challenge with LOGIN operation when generating login options")
        void shouldStoreChallengeWithLoginOperation_whenGeneratingLoginOptions() throws Exception {
            // Given
            AssertionRequest assertionRequest = mock(AssertionRequest.class);
            PublicKeyCredentialRequestOptions requestOptions = mock(PublicKeyCredentialRequestOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());

            when(relyingParty.startAssertion(argThat(opts -> true))).thenReturn(assertionRequest);
            when(assertionRequest.getPublicKeyCredentialRequestOptions()).thenReturn(requestOptions);
            when(requestOptions.getChallenge()).thenReturn(challenge);
            when(assertionRequest.toCredentialsGetJson()).thenReturn(CREDENTIALS_GET_JSON);
            when(assertionRequest.toJson()).thenReturn(OPTIONS_JSON);

            // When
            useCase.generateLoginOptions(TENANT_ID);

            // Then
            ArgumentCaptor<PasskeyChallengeStore.ChallengeMetadata> metadataCaptor =
                    ArgumentCaptor.forClass(PasskeyChallengeStore.ChallengeMetadata.class);
            verify(challengeStore).store(eq(challenge.getBase64Url()), metadataCaptor.capture());
            PasskeyChallengeStore.ChallengeMetadata captured = metadataCaptor.getValue();
            assertThat(captured.userId()).isNull();
            assertThat(captured.tenantId()).isEqualTo(TENANT_ID);
            assertThat(captured.operation()).isEqualTo(PasskeyChallengeStore.OPERATION_LOGIN);
        }

        @Test
        @DisplayName("Should store serialized assertion options in Redis")
        void shouldStoreSerializedAssertionOptions_whenGeneratingLoginOptions() throws Exception {
            // Given
            AssertionRequest assertionRequest = mock(AssertionRequest.class);
            PublicKeyCredentialRequestOptions requestOptions = mock(PublicKeyCredentialRequestOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());

            when(relyingParty.startAssertion(argThat(opts -> true))).thenReturn(assertionRequest);
            when(assertionRequest.getPublicKeyCredentialRequestOptions()).thenReturn(requestOptions);
            when(requestOptions.getChallenge()).thenReturn(challenge);
            when(assertionRequest.toCredentialsGetJson()).thenReturn(CREDENTIALS_GET_JSON);
            when(assertionRequest.toJson()).thenReturn(OPTIONS_JSON);

            // When
            useCase.generateLoginOptions(TENANT_ID);

            // Then
            verify(challengeStore).storeOptions(challenge.getBase64Url(), OPTIONS_JSON);
        }

        @Test
        @DisplayName("Should return credentials get JSON when generating login options")
        void shouldReturnCredentialsGetJson_whenGeneratingLoginOptions() throws Exception {
            // Given
            AssertionRequest assertionRequest = mock(AssertionRequest.class);
            PublicKeyCredentialRequestOptions requestOptions = mock(PublicKeyCredentialRequestOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());

            when(relyingParty.startAssertion(argThat(opts -> true))).thenReturn(assertionRequest);
            when(assertionRequest.getPublicKeyCredentialRequestOptions()).thenReturn(requestOptions);
            when(requestOptions.getChallenge()).thenReturn(challenge);
            when(assertionRequest.toCredentialsGetJson()).thenReturn(CREDENTIALS_GET_JSON);
            when(assertionRequest.toJson()).thenReturn(OPTIONS_JSON);

            // When
            String result = useCase.generateLoginOptions(TENANT_ID);

            // Then
            assertThat(result).isEqualTo(CREDENTIALS_GET_JSON);
        }
    }

    @Nested
    @DisplayName("Login Completion Error Paths")
    class LoginCompletionErrorPaths {

        @Test
        @DisplayName("Should throw PasskeyAuthenticationException when assertion response JSON is invalid")
        void shouldThrowPasskeyAuthenticationException_whenAssertionResponseJsonIsInvalid() {
            // Given
            String invalidJson = "not-valid-json";

            // When / Then
            assertThatThrownBy(() -> useCase.completeLogin(invalidJson, TENANT_ID))
                    .isInstanceOf(PasskeyAuthenticationException.class)
                    .hasMessageContaining(PasskeyAuthenticationUseCase.ERROR_INVALID_RESPONSE);
        }
    }
}
