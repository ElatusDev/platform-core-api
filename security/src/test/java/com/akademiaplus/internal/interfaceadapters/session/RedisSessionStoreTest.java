/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisSessionStore}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("RedisSessionStore")
@ExtendWith(MockitoExtension.class)
class RedisSessionStoreTest {

    private static final String JTI = "test-jti-uuid";
    private static final String USERNAME = "testuser";
    private static final Long TENANT_ID = 1L;
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String SESSION_KEY = RedisSessionStore.SESSION_KEY_PREFIX + JTI;
    private static final String USER_SESSIONS_KEY = RedisSessionStore.USER_SESSIONS_KEY_PREFIX + USERNAME + ":" + TENANT_ID;

    @Mock
    private RedisTemplate<String, String> sessionRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisSessionStore redisSessionStore;

    @BeforeEach
    void setUp() {
        redisSessionStore = new RedisSessionStore(sessionRedisTemplate);
    }

    @Nested
    @DisplayName("Session Storage")
    class SessionStorage {

        @Test
        @DisplayName("Should store session hash and add to user session set when valid parameters provided")
        void shouldStoreSession_whenValidParametersProvided() {
            // Given
            when(sessionRedisTemplate.opsForHash()).thenReturn(hashOperations);
            when(sessionRedisTemplate.opsForSet()).thenReturn(setOperations);

            // When
            redisSessionStore.storeSession(JTI, USERNAME, TENANT_ID, TTL);

            // Then
            verify(hashOperations).putAll(SESSION_KEY, Map.of(
                    RedisSessionStore.FIELD_USERNAME, USERNAME,
                    RedisSessionStore.FIELD_TENANT_ID, String.valueOf(TENANT_ID)
            ));
            verify(sessionRedisTemplate).expire(SESSION_KEY, TTL);
            verify(setOperations).add(USER_SESSIONS_KEY, JTI);
            verify(sessionRedisTemplate).expire(USER_SESSIONS_KEY, TTL);
        }
    }

    @Nested
    @DisplayName("Session Validation")
    class SessionValidation {

        @Test
        @DisplayName("Should return true when session exists in Redis")
        void shouldReturnTrue_whenSessionExists() {
            // Given
            when(sessionRedisTemplate.hasKey(SESSION_KEY)).thenReturn(Boolean.TRUE);

            // When
            boolean result = redisSessionStore.isSessionValid(JTI);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when session does not exist in Redis")
        void shouldReturnFalse_whenSessionDoesNotExist() {
            // Given
            when(sessionRedisTemplate.hasKey(SESSION_KEY)).thenReturn(Boolean.FALSE);

            // When
            boolean result = redisSessionStore.isSessionValid(JTI);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Session Revocation")
    class SessionRevocation {

        @Test
        @DisplayName("Should delete session key when revoking single session")
        void shouldDeleteSessionKey_whenRevokingSingleSession() {
            // Given — no setup needed

            // When
            redisSessionStore.revokeSession(JTI);

            // Then
            verify(sessionRedisTemplate).delete(SESSION_KEY);
        }

        @Test
        @DisplayName("Should delete all session keys for user when revoking by user")
        void shouldDeleteAllSessionsForUser_whenRevokingByUser() {
            // Given
            String jti1 = "jti-1";
            String jti2 = "jti-2";
            when(sessionRedisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members(USER_SESSIONS_KEY)).thenReturn(Set.of(jti1, jti2));

            // When
            redisSessionStore.revokeAllSessionsForUser(USERNAME, TENANT_ID);

            // Then
            verify(sessionRedisTemplate).delete(RedisSessionStore.SESSION_KEY_PREFIX + jti1);
            verify(sessionRedisTemplate).delete(RedisSessionStore.SESSION_KEY_PREFIX + jti2);
            verify(sessionRedisTemplate).delete(USER_SESSIONS_KEY);
        }
    }
}
