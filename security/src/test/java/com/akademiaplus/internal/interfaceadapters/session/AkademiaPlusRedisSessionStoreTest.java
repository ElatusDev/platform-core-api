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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.mockito.InOrder;

/**
 * Unit tests for {@link AkademiaPlusRedisSessionStore}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("AkademiaPlusRedisSessionStore")
@ExtendWith(MockitoExtension.class)
class AkademiaPlusRedisSessionStoreTest {

    private static final String JTI = "test-jti-uuid";
    private static final String USERNAME = "testuser";
    private static final Long TENANT_ID = 1L;
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String SESSION_KEY = AkademiaPlusRedisSessionStore.SESSION_KEY_PREFIX + JTI;
    private static final String USER_SESSIONS_KEY = AkademiaPlusRedisSessionStore.USER_SESSIONS_KEY_PREFIX + USERNAME + ":" + TENANT_ID;

    @Mock
    private RedisTemplate<String, String> akademiaPlusRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;

    @BeforeEach
    void setUp() {
        akademiaPlusRedisSessionStore = new AkademiaPlusRedisSessionStore(akademiaPlusRedisTemplate);
    }

    @Nested
    @DisplayName("Session Storage")
    class SessionStorage {

        @Test
        @DisplayName("Should store session hash and add to user session set when valid parameters provided")
        void shouldStoreSession_whenValidParametersProvided() {
            // Given
            when(akademiaPlusRedisTemplate.opsForHash()).thenReturn(hashOperations);
            when(akademiaPlusRedisTemplate.opsForSet()).thenReturn(setOperations);

            // When
            akademiaPlusRedisSessionStore.storeSession(JTI, USERNAME, TENANT_ID, TTL);

            // Then
            assertThat(akademiaPlusRedisSessionStore).isNotNull();
            InOrder inOrder = inOrder(akademiaPlusRedisTemplate, hashOperations, setOperations);
            inOrder.verify(akademiaPlusRedisTemplate, times(1)).opsForHash();
            inOrder.verify(hashOperations, times(1)).putAll(SESSION_KEY, Map.of(
                    AkademiaPlusRedisSessionStore.FIELD_USERNAME, USERNAME,
                    AkademiaPlusRedisSessionStore.FIELD_TENANT_ID, String.valueOf(TENANT_ID)
            ));
            inOrder.verify(akademiaPlusRedisTemplate, times(1)).expire(SESSION_KEY, TTL);
            inOrder.verify(akademiaPlusRedisTemplate, times(1)).opsForSet();
            inOrder.verify(setOperations, times(1)).add(USER_SESSIONS_KEY, JTI);
            inOrder.verify(akademiaPlusRedisTemplate, times(1)).expire(USER_SESSIONS_KEY, TTL);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }
    }

    @Nested
    @DisplayName("Session Validation")
    class SessionValidation {

        @Test
        @DisplayName("Should return true when session exists in Redis")
        void shouldReturnTrue_whenSessionExists() {
            // Given
            when(akademiaPlusRedisTemplate.hasKey(SESSION_KEY)).thenReturn(Boolean.TRUE);

            // When
            boolean result = akademiaPlusRedisSessionStore.isSessionValid(JTI);

            // Then
            assertThat(result).isTrue();
            verify(akademiaPlusRedisTemplate, times(1)).hasKey(SESSION_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }

        @Test
        @DisplayName("Should return false when session does not exist in Redis")
        void shouldReturnFalse_whenSessionDoesNotExist() {
            // Given
            when(akademiaPlusRedisTemplate.hasKey(SESSION_KEY)).thenReturn(Boolean.FALSE);

            // When
            boolean result = akademiaPlusRedisSessionStore.isSessionValid(JTI);

            // Then
            assertThat(result).isFalse();
            verify(akademiaPlusRedisTemplate, times(1)).hasKey(SESSION_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
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
            akademiaPlusRedisSessionStore.revokeSession(JTI);

            // Then
            assertThat(akademiaPlusRedisSessionStore).isNotNull();
            verify(akademiaPlusRedisTemplate, times(1)).delete(SESSION_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }

        @Test
        @DisplayName("Should delete all session keys for user when revoking by user")
        void shouldDeleteAllSessionsForUser_whenRevokingByUser() {
            // Given
            String jti1 = "jti-1";
            String jti2 = "jti-2";
            when(akademiaPlusRedisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members(USER_SESSIONS_KEY)).thenReturn(Set.of(jti1, jti2));

            // When
            akademiaPlusRedisSessionStore.revokeAllSessionsForUser(USERNAME, TENANT_ID);

            // Then
            assertThat(akademiaPlusRedisSessionStore).isNotNull();
            verify(akademiaPlusRedisTemplate, times(1)).opsForSet();
            verify(setOperations, times(1)).members(USER_SESSIONS_KEY);
            verify(akademiaPlusRedisTemplate, times(1)).delete(AkademiaPlusRedisSessionStore.SESSION_KEY_PREFIX + jti1);
            verify(akademiaPlusRedisTemplate, times(1)).delete(AkademiaPlusRedisSessionStore.SESSION_KEY_PREFIX + jti2);
            verify(akademiaPlusRedisTemplate, times(1)).delete(USER_SESSIONS_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }

        @Test
        @DisplayName("Should delete user sessions key even when no individual sessions exist")
        void shouldDeleteUserSessionsKey_whenNoIndividualSessionsExist() {
            // Given
            when(akademiaPlusRedisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.members(USER_SESSIONS_KEY)).thenReturn(null);

            // When
            akademiaPlusRedisSessionStore.revokeAllSessionsForUser(USERNAME, TENANT_ID);

            // Then
            assertThat(akademiaPlusRedisSessionStore).isNotNull();
            verify(akademiaPlusRedisTemplate, times(1)).opsForSet();
            verify(setOperations, times(1)).members(USER_SESSIONS_KEY);
            verify(akademiaPlusRedisTemplate, times(1)).delete(USER_SESSIONS_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when redisTemplate throws on storeSession")
        void shouldPropagateException_whenRedisTemplateThrowsOnStoreSession() {
            // Given
            RuntimeException cause = new RuntimeException("Redis connection error");
            when(akademiaPlusRedisTemplate.opsForHash()).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> akademiaPlusRedisSessionStore.storeSession(JTI, USERNAME, TENANT_ID, TTL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Redis connection error");

            verify(akademiaPlusRedisTemplate, times(1)).opsForHash();
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }

        @Test
        @DisplayName("Should propagate exception when redisTemplate throws on isSessionValid")
        void shouldPropagateException_whenRedisTemplateThrowsOnIsSessionValid() {
            // Given
            RuntimeException cause = new RuntimeException("Redis connection error");
            when(akademiaPlusRedisTemplate.hasKey(SESSION_KEY)).thenThrow(cause);

            // When / Then
            assertThatThrownBy(() -> akademiaPlusRedisSessionStore.isSessionValid(JTI))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Redis connection error");

            verify(akademiaPlusRedisTemplate, times(1)).hasKey(SESSION_KEY);
            verifyNoMoreInteractions(akademiaPlusRedisTemplate, hashOperations, setOperations);
        }
    }
}
