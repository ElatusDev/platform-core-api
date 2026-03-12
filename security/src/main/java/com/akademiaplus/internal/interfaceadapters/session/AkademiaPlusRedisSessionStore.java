/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Redis-backed session store for JWT access token session management.
 *
 * <p>Stores session metadata (username, tenantId) keyed by JTI (JWT ID).
 * Maintains a secondary index of all sessions per user for bulk revocation.
 * All keys have TTL matching the access token validity.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class AkademiaPlusRedisSessionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkademiaPlusRedisSessionStore.class);

    /** Prefix for individual session keys: {@code session:{jti}}. */
    public static final String SESSION_KEY_PREFIX = "session:";

    /** Prefix for per-user session set keys: {@code user_sessions:{username}:{tenantId}}. */
    public static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";

    /** Hash field name for the user ID. */
    public static final String FIELD_USER_ID = "userId";

    /** Hash field name for the tenant ID. */
    public static final String FIELD_TENANT_ID = "tenantId";

    /** Hash field name for the username. */
    public static final String FIELD_USERNAME = "username";

    private final RedisTemplate<String, String> akademiaPlusRedisTemplate;

    /**
     * Constructs a AkademiaPlusRedisSessionStore with the required Redis template.
     *
     * @param akademiaPlusRedisTemplate the Redis template for session storage
     */
    public AkademiaPlusRedisSessionStore(RedisTemplate<String, String> akademiaPlusRedisTemplate) {
        this.akademiaPlusRedisTemplate = akademiaPlusRedisTemplate;
    }

    /**
     * Stores a new session in Redis with the given TTL.
     *
     * <p>Creates a hash at {@code session:{jti}} with username and tenantId fields,
     * and adds the JTI to the user's session set at {@code user_sessions:{username}:{tenantId}}.</p>
     *
     * @param jti      the unique JWT ID
     * @param username the authenticated username
     * @param tenantId the tenant ID
     * @param ttl      the time-to-live for the session entry
     */
    public void storeSession(String jti, String username, Long tenantId, Duration ttl) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + username + ":" + tenantId;

        akademiaPlusRedisTemplate.opsForHash().putAll(sessionKey, Map.of(
                FIELD_USERNAME, username,
                FIELD_TENANT_ID, String.valueOf(tenantId)
        ));
        akademiaPlusRedisTemplate.expire(sessionKey, ttl);

        akademiaPlusRedisTemplate.opsForSet().add(userSessionsKey, jti);
        akademiaPlusRedisTemplate.expire(userSessionsKey, ttl);

        LOGGER.debug("Stored session for user {} tenant {} with jti {}", username, tenantId, jti);
    }

    /**
     * Checks whether a session with the given JTI exists in Redis.
     *
     * @param jti the JWT ID to check
     * @return {@code true} if the session exists, {@code false} otherwise
     */
    public boolean isSessionValid(String jti) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        return Boolean.TRUE.equals(akademiaPlusRedisTemplate.hasKey(sessionKey));
    }

    /**
     * Revokes a single session by deleting its Redis key.
     *
     * @param jti the JWT ID of the session to revoke
     */
    public void revokeSession(String jti) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        akademiaPlusRedisTemplate.delete(sessionKey);
        LOGGER.debug("Revoked session with jti {}", jti);
    }

    /**
     * Revokes all sessions for a given user and tenant.
     *
     * <p>Retrieves all JTIs from the user's session set, deletes each
     * individual session key, then deletes the session set itself.</p>
     *
     * @param username the username whose sessions should be revoked
     * @param tenantId the tenant ID
     */
    public void revokeAllSessionsForUser(String username, Long tenantId) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + username + ":" + tenantId;
        Set<String> sessionIds = akademiaPlusRedisTemplate.opsForSet().members(userSessionsKey);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (String jti : sessionIds) {
                akademiaPlusRedisTemplate.delete(SESSION_KEY_PREFIX + jti);
            }
        }
        akademiaPlusRedisTemplate.delete(userSessionsKey);
        LOGGER.debug("Revoked all sessions for user {} tenant {}", username, tenantId);
    }
}
