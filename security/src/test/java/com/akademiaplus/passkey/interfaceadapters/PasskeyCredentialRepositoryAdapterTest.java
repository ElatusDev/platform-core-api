/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasskeyCredentialRepositoryAdapter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyCredentialRepositoryAdapter")
@ExtendWith(MockitoExtension.class)
class PasskeyCredentialRepositoryAdapterTest {

    private static final String USERNAME = "testuser@example.com";
    private static final String USERNAME_HASH = "abc123hash";
    private static final Long USER_ID = 42L;
    private static final byte[] CREDENTIAL_ID_BYTES = new byte[]{1, 2, 3, 4, 5};
    private static final byte[] PUBLIC_KEY_BYTES = new byte[]{10, 20, 30, 40, 50};
    private static final byte[] USER_HANDLE_BYTES = new byte[]{100, 101, 102, 103};
    private static final long SIGN_COUNT = 5L;

    @Mock
    private PasskeyCredentialJpaRepository jpaRepository;

    @Mock
    private InternalAuthRepository internalAuthRepository;

    @Mock
    private HashingService hashingService;

    private PasskeyCredentialRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PasskeyCredentialRepositoryAdapter(jpaRepository, internalAuthRepository, hashingService);
    }

    private PasskeyCredentialDataModel createCredential() {
        PasskeyCredentialDataModel credential = new PasskeyCredentialDataModel();
        credential.setUserId(USER_ID);
        credential.setCredentialId(CREDENTIAL_ID_BYTES);
        credential.setPublicKey(PUBLIC_KEY_BYTES);
        credential.setUserHandle(USER_HANDLE_BYTES);
        credential.setSignCount(SIGN_COUNT);
        return credential;
    }

    private InternalAuthDataModel createAuthDataModel() {
        InternalAuthDataModel auth = new InternalAuthDataModel();
        auth.setInternalAuthId(USER_ID);
        auth.setUsername(USERNAME);
        auth.setUsernameHash(USERNAME_HASH);
        return auth;
    }

    @Nested
    @DisplayName("Credential Lookup by Username")
    class CredentialLookup {

        @Test
        @DisplayName("Should return credential IDs when user has registered passkeys")
        void shouldReturnCredentialIds_whenUserHasRegisteredPasskeys() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(jpaRepository.findByUserId(USER_ID)).thenReturn(List.of(createCredential()));

            // When
            Set<PublicKeyCredentialDescriptor> result = adapter.getCredentialIdsForUsername(USERNAME);

            // Then
            assertThat(result).hasSize(1);
            PublicKeyCredentialDescriptor descriptor = result.iterator().next();
            assertThat(descriptor.getId().getBytes()).isEqualTo(CREDENTIAL_ID_BYTES);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty set when user has no passkeys")
        void shouldReturnEmptySet_whenUserHasNoPasskeys() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(jpaRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Set<PublicKeyCredentialDescriptor> result = adapter.getCredentialIdsForUsername(USERNAME);

            // Then
            assertThat(result).isEmpty();
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty set when username is not found")
        void shouldReturnEmptySet_whenUsernameNotFound() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.empty());

            // When
            Set<PublicKeyCredentialDescriptor> result = adapter.getCredentialIdsForUsername(USERNAME);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(jpaRepository);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }
    }

    @Nested
    @DisplayName("User Handle Lookup")
    class UserHandleLookup {

        @Test
        @DisplayName("Should return user handle when username exists and has credentials")
        void shouldReturnUserHandle_whenUsernameExists() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(jpaRepository.findByUserId(USER_ID)).thenReturn(List.of(createCredential()));

            // When
            Optional<ByteArray> result = adapter.getUserHandleForUsername(USERNAME);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getBytes()).isEqualTo(USER_HANDLE_BYTES);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty when username is not found")
        void shouldReturnEmpty_whenUsernameNotFound() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.empty());

            // When
            Optional<ByteArray> result = adapter.getUserHandleForUsername(USERNAME);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(jpaRepository);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty when user has no credentials")
        void shouldReturnEmpty_whenUserHasNoCredentials() {
            // Given
            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(createAuthDataModel()));
            when(jpaRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // When
            Optional<ByteArray> result = adapter.getUserHandleForUsername(USERNAME);

            // Then
            assertThat(result).isEmpty();
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }
    }

    @Nested
    @DisplayName("Username for User Handle")
    class UsernameForUserHandle {

        @Test
        @DisplayName("Should return username when user handle matches a credential")
        void shouldReturnUsername_whenUserHandleMatchesCredential() {
            // Given
            when(jpaRepository.findByUserHandle(USER_HANDLE_BYTES))
                    .thenReturn(List.of(createCredential()));
            when(internalAuthRepository.findByInternalAuthId(USER_ID))
                    .thenReturn(Optional.of(createAuthDataModel()));

            // When
            Optional<String> result = adapter.getUsernameForUserHandle(new ByteArray(USER_HANDLE_BYTES));

            // Then
            assertThat(result).isPresent().contains(USERNAME);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty when no credentials match user handle")
        void shouldReturnEmpty_whenNoCredentialsMatchUserHandle() {
            // Given
            when(jpaRepository.findByUserHandle(USER_HANDLE_BYTES)).thenReturn(Collections.emptyList());

            // When
            Optional<String> result = adapter.getUsernameForUserHandle(new ByteArray(USER_HANDLE_BYTES));

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }
    }

    @Nested
    @DisplayName("Assertion Lookup")
    class AssertionLookup {

        @Test
        @DisplayName("Should return registered credential when credential ID and user handle match")
        void shouldReturnRegisteredCredential_whenCredentialIdAndUserHandleMatch() {
            // Given
            when(jpaRepository.findByCredentialId(CREDENTIAL_ID_BYTES))
                    .thenReturn(Optional.of(createCredential()));

            // When
            Optional<RegisteredCredential> result = adapter.lookup(
                    new ByteArray(CREDENTIAL_ID_BYTES), new ByteArray(USER_HANDLE_BYTES));

            // Then
            assertThat(result).isPresent();
            RegisteredCredential registered = result.get();
            assertThat(registered.getCredentialId().getBytes()).isEqualTo(CREDENTIAL_ID_BYTES);
            assertThat(registered.getUserHandle().getBytes()).isEqualTo(USER_HANDLE_BYTES);
            assertThat(registered.getPublicKeyCose().getBytes()).isEqualTo(PUBLIC_KEY_BYTES);
            assertThat(registered.getSignatureCount()).isEqualTo(SIGN_COUNT);
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty when credential ID is not found")
        void shouldReturnEmpty_whenCredentialIdNotFound() {
            // Given
            when(jpaRepository.findByCredentialId(CREDENTIAL_ID_BYTES))
                    .thenReturn(Optional.empty());

            // When
            Optional<RegisteredCredential> result = adapter.lookup(
                    new ByteArray(CREDENTIAL_ID_BYTES), new ByteArray(USER_HANDLE_BYTES));

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty when credential ID exists but user handle does not match")
        void shouldReturnEmpty_whenCredentialIdExistsButUserHandleDoesNotMatch() {
            // Given
            when(jpaRepository.findByCredentialId(CREDENTIAL_ID_BYTES))
                    .thenReturn(Optional.of(createCredential()));
            byte[] differentUserHandle = new byte[]{99, 98, 97, 96};

            // When
            Optional<RegisteredCredential> result = adapter.lookup(
                    new ByteArray(CREDENTIAL_ID_BYTES), new ByteArray(differentUserHandle));

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }
    }

    @Nested
    @DisplayName("Lookup All")
    class LookupAll {

        @Test
        @DisplayName("Should return set with credential when credential ID exists")
        void shouldReturnSetWithCredential_whenCredentialIdExists() {
            // Given
            when(jpaRepository.findByCredentialId(CREDENTIAL_ID_BYTES))
                    .thenReturn(Optional.of(createCredential()));

            // When
            Set<RegisteredCredential> result = adapter.lookupAll(new ByteArray(CREDENTIAL_ID_BYTES));

            // Then
            assertThat(result).hasSize(1);
            RegisteredCredential registered = result.iterator().next();
            assertThat(registered.getCredentialId().getBytes()).isEqualTo(CREDENTIAL_ID_BYTES);
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }

        @Test
        @DisplayName("Should return empty set when credential ID is not found")
        void shouldReturnEmptySet_whenCredentialIdNotFound() {
            // Given
            when(jpaRepository.findByCredentialId(CREDENTIAL_ID_BYTES))
                    .thenReturn(Optional.empty());

            // When
            Set<RegisteredCredential> result = adapter.lookupAll(new ByteArray(CREDENTIAL_ID_BYTES));

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(internalAuthRepository, hashingService);
            verifyNoMoreInteractions(jpaRepository, internalAuthRepository, hashingService);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when hashingService throws on getCredentialIdsForUsername")
        void shouldPropagateException_whenHashingServiceThrowsOnGetCredentialIds() {
            // Given
            RuntimeException cause = new RuntimeException("hash failure");
            when(hashingService.generateHash(USERNAME)).thenThrow(cause);

            // When / Then
            assertThat(org.assertj.core.api.Assertions.catchThrowable(
                    () -> adapter.getCredentialIdsForUsername(USERNAME)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hash failure");

            verify(hashingService, times(1)).generateHash(USERNAME);
            verifyNoInteractions(jpaRepository, internalAuthRepository);
        }

        @Test
        @DisplayName("Should propagate exception when internalAuthRepository throws on getUsernameForUserHandle")
        void shouldPropagateException_whenInternalAuthRepositoryThrows() {
            // Given
            when(jpaRepository.findByUserHandle(USER_HANDLE_BYTES))
                    .thenReturn(List.of(createCredential()));
            RuntimeException cause = new RuntimeException("db error");
            when(internalAuthRepository.findByInternalAuthId(USER_ID)).thenThrow(cause);

            // When / Then
            assertThat(org.assertj.core.api.Assertions.catchThrowable(
                    () -> adapter.getUsernameForUserHandle(new ByteArray(USER_HANDLE_BYTES))))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db error");

            verify(jpaRepository, times(1)).findByUserHandle(USER_HANDLE_BYTES);
            verify(internalAuthRepository, times(1)).findByInternalAuthId(USER_ID);
            verifyNoInteractions(hashingService);
        }
    }
}
