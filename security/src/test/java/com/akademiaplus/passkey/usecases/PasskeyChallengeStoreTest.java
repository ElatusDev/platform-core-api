/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.usecases;

import com.akademiaplus.passkey.config.PasskeyProperties;
import com.akademiaplus.passkey.exceptions.PasskeyAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasskeyChallengeStore}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyChallengeStore")
@ExtendWith(MockitoExtension.class)
class PasskeyChallengeStoreTest {

    private static final String CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2U";
    private static final Long USER_ID = 42L;
    private static final Long TENANT_ID = 1L;
    private static final Long CHALLENGE_TTL_SECONDS = 300L;
    private static final String CHALLENGE_KEY = PasskeyChallengeStore.KEY_PREFIX + CHALLENGE_BASE64;
    private static final String OPTIONS_KEY = PasskeyChallengeStore.KEY_PREFIX + "options:" + CHALLENGE_BASE64;
    private static final String OPTIONS_JSON = "{\"challenge\":\"dGVzdC1jaGFsbGVuZ2U\"}";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasskeyProperties properties;

    private PasskeyChallengeStore challengeStore;

    @BeforeEach
    void setUp() {
        challengeStore = new PasskeyChallengeStore(redisTemplate, properties);
    }

    @Nested
    @DisplayName("Challenge Storage")
    class Storage {

        @Test
        @DisplayName("Should store challenge metadata in Redis when called with valid metadata")
        void shouldStoreChallenge_whenCalledWithValidMetadata() {
            // Given
            PasskeyChallengeStore.ChallengeMetadata metadata = new PasskeyChallengeStore.ChallengeMetadata(
                    USER_ID, TENANT_ID, PasskeyChallengeStore.OPERATION_REGISTER);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(properties.getChallengeTtlSeconds()).thenReturn(CHALLENGE_TTL_SECONDS);

            // When
            challengeStore.store(CHALLENGE_BASE64, metadata);

            // Then
            String expectedValue = USER_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + TENANT_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + PasskeyChallengeStore.OPERATION_REGISTER;
            verify(valueOperations, times(1)).set(CHALLENGE_KEY, expectedValue, Duration.ofSeconds(CHALLENGE_TTL_SECONDS));
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should set TTL from properties when storing challenge")
        void shouldSetTtl_whenStoringChallenge() {
            // Given
            PasskeyChallengeStore.ChallengeMetadata metadata = new PasskeyChallengeStore.ChallengeMetadata(
                    null, TENANT_ID, PasskeyChallengeStore.OPERATION_LOGIN);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(properties.getChallengeTtlSeconds()).thenReturn(600L);

            // When
            challengeStore.store(CHALLENGE_BASE64, metadata);

            // Then
            String expectedValue = PasskeyChallengeStore.METADATA_DELIMITER
                    + TENANT_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + PasskeyChallengeStore.OPERATION_LOGIN;
            verify(valueOperations, times(1)).set(CHALLENGE_KEY, expectedValue, Duration.ofSeconds(600L));
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }
    }

    @Nested
    @DisplayName("Challenge Consumption")
    class Consumption {

        @Test
        @DisplayName("Should return metadata when challenge exists in Redis")
        void shouldReturnMetadata_whenChallengeExists() {
            // Given
            String storedValue = USER_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + TENANT_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + PasskeyChallengeStore.OPERATION_REGISTER;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(CHALLENGE_KEY)).thenReturn(storedValue);

            // When
            PasskeyChallengeStore.ChallengeMetadata result = challengeStore.consumeChallenge(CHALLENGE_BASE64);

            // Then
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.tenantId()).isEqualTo(TENANT_ID);
            assertThat(result.operation()).isEqualTo(PasskeyChallengeStore.OPERATION_REGISTER);
            verify(valueOperations, times(1)).getAndDelete(CHALLENGE_KEY);
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should delete challenge from Redis when consumed")
        void shouldDeleteChallenge_whenConsumed() {
            // Given
            String storedValue = USER_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + TENANT_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + PasskeyChallengeStore.OPERATION_REGISTER;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(CHALLENGE_KEY)).thenReturn(storedValue);

            // When
            challengeStore.consumeChallenge(CHALLENGE_BASE64);

            // Then
            verify(valueOperations, times(1)).getAndDelete(CHALLENGE_KEY);
            assertThat(challengeStore).isNotNull();
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should return null userId when login challenge has no userId")
        void shouldReturnNullUserId_whenLoginChallengeHasNoUserId() {
            // Given
            String storedValue = PasskeyChallengeStore.METADATA_DELIMITER
                    + TENANT_ID + PasskeyChallengeStore.METADATA_DELIMITER
                    + PasskeyChallengeStore.OPERATION_LOGIN;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(CHALLENGE_KEY)).thenReturn(storedValue);

            // When
            PasskeyChallengeStore.ChallengeMetadata result = challengeStore.consumeChallenge(CHALLENGE_BASE64);

            // Then
            assertThat(result.userId()).isNull();
            assertThat(result.tenantId()).isEqualTo(TENANT_ID);
            assertThat(result.operation()).isEqualTo(PasskeyChallengeStore.OPERATION_LOGIN);
            verify(valueOperations, times(1)).getAndDelete(CHALLENGE_KEY);
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should throw PasskeyAuthenticationException when challenge is not found")
        void shouldThrowPasskeyAuthenticationException_whenChallengeNotFound() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(CHALLENGE_KEY)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> challengeStore.consumeChallenge(CHALLENGE_BASE64))
                    .isInstanceOf(PasskeyAuthenticationException.class)
                    .hasMessage(PasskeyChallengeStore.ERROR_CHALLENGE_NOT_FOUND);
            verify(valueOperations, times(1)).getAndDelete(CHALLENGE_KEY);
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }
    }

    @Nested
    @DisplayName("Options Storage")
    class OptionsStorage {

        @Test
        @DisplayName("Should store options JSON in Redis when called with valid data")
        void shouldStoreOptionsJson_whenCalledWithValidData() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(properties.getChallengeTtlSeconds()).thenReturn(CHALLENGE_TTL_SECONDS);

            // When
            challengeStore.storeOptions(CHALLENGE_BASE64, OPTIONS_JSON);

            // Then
            verify(valueOperations, times(1)).set(OPTIONS_KEY, OPTIONS_JSON, Duration.ofSeconds(CHALLENGE_TTL_SECONDS));
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should return options JSON when consumed")
        void shouldReturnOptionsJson_whenConsumed() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(OPTIONS_KEY)).thenReturn(OPTIONS_JSON);

            // When
            String result = challengeStore.consumeOptions(CHALLENGE_BASE64);

            // Then
            assertThat(result).isEqualTo(OPTIONS_JSON);
            verify(valueOperations, times(1)).getAndDelete(OPTIONS_KEY);
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }

        @Test
        @DisplayName("Should throw PasskeyAuthenticationException when options not found")
        void shouldThrowPasskeyAuthenticationException_whenOptionsNotFound() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(OPTIONS_KEY)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> challengeStore.consumeOptions(CHALLENGE_BASE64))
                    .isInstanceOf(PasskeyAuthenticationException.class)
                    .hasMessage(PasskeyChallengeStore.ERROR_CHALLENGE_NOT_FOUND);
            verify(valueOperations, times(1)).getAndDelete(OPTIONS_KEY);
            verifyNoMoreInteractions(redisTemplate, valueOperations, properties);
        }
    }
}
