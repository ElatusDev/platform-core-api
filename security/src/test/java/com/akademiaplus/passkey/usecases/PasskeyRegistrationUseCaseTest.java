/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.usecases;

import com.akademiaplus.passkey.exceptions.PasskeyRegistrationException;
import com.akademiaplus.passkey.interfaceadapters.PasskeyCredentialJpaRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasskeyRegistrationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyRegistrationUseCase")
@ExtendWith(MockitoExtension.class)
class PasskeyRegistrationUseCaseTest {

    private static final Long USER_ID = 42L;
    private static final String USERNAME = "testuser@example.com";
    private static final byte[] USER_HANDLE = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    private static final Long TENANT_ID = 1L;
    private static final String DISPLAY_NAME = "My Passkey";
    private static final String CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2U";
    private static final String OPTIONS_JSON = "{\"rp\":{\"name\":\"test\"}}";
    private static final String CREDENTIALS_CREATE_JSON = "{\"publicKey\":{}}";

    @Mock
    private RelyingParty relyingParty;

    @Mock
    private PasskeyChallengeStore challengeStore;

    @Mock
    private PasskeyCredentialJpaRepository credentialRepository;

    @Mock
    private ApplicationContext applicationContext;

    private PasskeyRegistrationUseCase registrationUseCase;

    @BeforeEach
    void setUp() {
        registrationUseCase = new PasskeyRegistrationUseCase(
                relyingParty, challengeStore, credentialRepository, applicationContext);
    }

    @Nested
    @DisplayName("Options Generation")
    class OptionsGeneration {

        @Test
        @DisplayName("Should store challenge metadata when options are generated")
        void shouldStoreChallengeMetadata_whenOptionsGenerated() throws Exception {
            // Given
            PublicKeyCredentialCreationOptions creationOptions = mock(PublicKeyCredentialCreationOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());
            when(creationOptions.getChallenge()).thenReturn(challenge);
            when(creationOptions.toCredentialsCreateJson()).thenReturn(CREDENTIALS_CREATE_JSON);
            when(creationOptions.toJson()).thenReturn(OPTIONS_JSON);
            when(relyingParty.startRegistration(argThat(opts ->
                    opts.getUser().getName().equals(USERNAME)))).thenReturn(creationOptions);

            // When
            registrationUseCase.generateRegistrationOptions(USER_ID, USERNAME, USER_HANDLE, TENANT_ID);

            // Then
            ArgumentCaptor<PasskeyChallengeStore.ChallengeMetadata> metadataCaptor =
                    ArgumentCaptor.forClass(PasskeyChallengeStore.ChallengeMetadata.class);
            verify(challengeStore, times(1)).store(eq(challenge.getBase64Url()), metadataCaptor.capture());
            PasskeyChallengeStore.ChallengeMetadata captured = metadataCaptor.getValue();
            assertThat(captured.userId()).isEqualTo(USER_ID);
            assertThat(captured.tenantId()).isEqualTo(TENANT_ID);
            assertThat(captured.operation()).isEqualTo(PasskeyChallengeStore.OPERATION_REGISTER);
            verify(challengeStore, times(1)).storeOptions(challenge.getBase64Url(), OPTIONS_JSON);
            verifyNoInteractions(credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }

        @Test
        @DisplayName("Should store serialized options in Redis when options are generated")
        void shouldStoreSerializedOptions_whenOptionsGenerated() throws Exception {
            // Given
            PublicKeyCredentialCreationOptions creationOptions = mock(PublicKeyCredentialCreationOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());
            when(creationOptions.getChallenge()).thenReturn(challenge);
            when(creationOptions.toCredentialsCreateJson()).thenReturn(CREDENTIALS_CREATE_JSON);
            when(creationOptions.toJson()).thenReturn(OPTIONS_JSON);
            when(relyingParty.startRegistration(argThat(opts ->
                    opts.getUser().getName().equals(USERNAME)))).thenReturn(creationOptions);

            // When
            registrationUseCase.generateRegistrationOptions(USER_ID, USERNAME, USER_HANDLE, TENANT_ID);

            // Then
            verify(challengeStore, times(1)).storeOptions(challenge.getBase64Url(), OPTIONS_JSON);
            verify(challengeStore, times(1)).store(eq(challenge.getBase64Url()), any(PasskeyChallengeStore.ChallengeMetadata.class));
            verifyNoInteractions(credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }

        @Test
        @DisplayName("Should return credentials create JSON when options are generated")
        void shouldReturnCredentialsCreateJson_whenOptionsGenerated() throws Exception {
            // Given
            PublicKeyCredentialCreationOptions creationOptions = mock(PublicKeyCredentialCreationOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());
            when(creationOptions.getChallenge()).thenReturn(challenge);
            when(creationOptions.toCredentialsCreateJson()).thenReturn(CREDENTIALS_CREATE_JSON);
            when(creationOptions.toJson()).thenReturn(OPTIONS_JSON);
            when(relyingParty.startRegistration(argThat(opts ->
                    opts.getUser().getName().equals(USERNAME)))).thenReturn(creationOptions);

            // When
            String result = registrationUseCase.generateRegistrationOptions(
                    USER_ID, USERNAME, USER_HANDLE, TENANT_ID);

            // Then
            assertThat(result).isEqualTo(CREDENTIALS_CREATE_JSON);
            verify(challengeStore, times(1)).store(eq(challenge.getBase64Url()), any(PasskeyChallengeStore.ChallengeMetadata.class));
            verify(challengeStore, times(1)).storeOptions(challenge.getBase64Url(), OPTIONS_JSON);
            verifyNoInteractions(credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }

        @Test
        @DisplayName("Should build UserIdentity with username and user handle")
        void shouldBuildUserIdentity_whenGeneratingOptions() throws Exception {
            // Given
            PublicKeyCredentialCreationOptions creationOptions = mock(PublicKeyCredentialCreationOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());
            when(creationOptions.getChallenge()).thenReturn(challenge);
            when(creationOptions.toCredentialsCreateJson()).thenReturn(CREDENTIALS_CREATE_JSON);
            when(creationOptions.toJson()).thenReturn(OPTIONS_JSON);

            ArgumentCaptor<StartRegistrationOptions> optionsCaptor =
                    ArgumentCaptor.forClass(StartRegistrationOptions.class);
            when(relyingParty.startRegistration(optionsCaptor.capture())).thenReturn(creationOptions);

            // When
            registrationUseCase.generateRegistrationOptions(USER_ID, USERNAME, USER_HANDLE, TENANT_ID);

            // Then
            StartRegistrationOptions captured = optionsCaptor.getValue();
            assertThat(captured.getUser().getName()).isEqualTo(USERNAME);
            assertThat(captured.getUser().getDisplayName()).isEqualTo(USERNAME);
            assertThat(captured.getUser().getId().getBytes()).isEqualTo(USER_HANDLE);
            verify(challengeStore, times(1)).store(eq(challenge.getBase64Url()), any(PasskeyChallengeStore.ChallengeMetadata.class));
            verify(challengeStore, times(1)).storeOptions(challenge.getBase64Url(), OPTIONS_JSON);
            verifyNoInteractions(credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }
    }

    @Nested
    @DisplayName("Registration Completion")
    class RegistrationCompletion {

        @Test
        @DisplayName("Should throw PasskeyRegistrationException when response JSON is invalid")
        void shouldThrowPasskeyRegistrationException_whenResponseJsonIsInvalid() {
            // Given
            String invalidJson = "not-valid-json";

            // When / Then
            assertThatThrownBy(() -> registrationUseCase.completeRegistration(
                    invalidJson, TENANT_ID, DISPLAY_NAME))
                    .isInstanceOf(PasskeyRegistrationException.class)
                    .hasMessageContaining(PasskeyRegistrationUseCase.ERROR_INVALID_RESPONSE);
            verifyNoInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }
    }

    @Nested
    @DisplayName("Error Paths")
    class ErrorPaths {

        @Test
        @DisplayName("Should throw PasskeyRegistrationException with serialization message when options serialization fails")
        void shouldThrowPasskeyRegistrationException_whenOptionsSerializationFails() throws Exception {
            // Given
            PublicKeyCredentialCreationOptions creationOptions = mock(PublicKeyCredentialCreationOptions.class);
            ByteArray challenge = new ByteArray(CHALLENGE_BASE64.getBytes());
            when(creationOptions.getChallenge()).thenReturn(challenge);
            when(creationOptions.toCredentialsCreateJson())
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("test") {});
            when(relyingParty.startRegistration(argThat(opts ->
                    opts.getUser().getName().equals(USERNAME)))).thenReturn(creationOptions);

            // When / Then
            assertThatThrownBy(() -> registrationUseCase.generateRegistrationOptions(
                    USER_ID, USERNAME, USER_HANDLE, TENANT_ID))
                    .isInstanceOf(PasskeyRegistrationException.class)
                    .hasMessageContaining(PasskeyRegistrationUseCase.ERROR_OPTIONS_SERIALIZATION_FAILED);
            verify(challengeStore, times(1)).store(eq(challenge.getBase64Url()), any(PasskeyChallengeStore.ChallengeMetadata.class));
            verifyNoInteractions(credentialRepository, applicationContext);
            verifyNoMoreInteractions(relyingParty, challengeStore, credentialRepository, applicationContext);
        }
    }
}
