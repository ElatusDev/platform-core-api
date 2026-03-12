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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed store for WebAuthn challenges with automatic expiration.
 *
 * <p>Challenges are stored with a configurable TTL (default 5 minutes)
 * to prevent replay attacks. Key format:
 * {@code passkey:challenge:{challengeBase64}}.
 *
 * <p>Each entry stores the challenge metadata (userId, tenantId, operation)
 * as a delimited string for retrieval during the complete step.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@ConditionalOnProperty(prefix = "security.passkey", name = "rp-id")
public class PasskeyChallengeStore {

    /** Redis key prefix for passkey challenges. */
    public static final String KEY_PREFIX = "passkey:challenge:";

    /** Error message when challenge is not found or expired. */
    public static final String ERROR_CHALLENGE_NOT_FOUND = "Challenge not found or expired";

    /** Delimiter for metadata serialization. */
    public static final String METADATA_DELIMITER = "|";

    /** Operation type for registration. */
    public static final String OPERATION_REGISTER = "REGISTER";

    /** Operation type for login. */
    public static final String OPERATION_LOGIN = "LOGIN";

    /** Expected number of metadata parts when deserializing. */
    private static final int METADATA_PARTS_COUNT = 3;

    private final StringRedisTemplate redisTemplate;
    private final PasskeyProperties properties;

    /**
     * Constructs the challenge store.
     *
     * @param redisTemplate the Redis template for string operations
     * @param properties    the passkey configuration properties
     */
    public PasskeyChallengeStore(StringRedisTemplate redisTemplate, PasskeyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Stores a challenge with its associated metadata.
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @param metadata        the challenge metadata
     */
    public void store(String challengeBase64, ChallengeMetadata metadata) {
        String key = KEY_PREFIX + challengeBase64;
        String value = serializeMetadata(metadata);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(properties.getChallengeTtlSeconds()));
    }

    /**
     * Retrieves and removes a challenge (single-use).
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @return the challenge metadata
     * @throws PasskeyAuthenticationException if the challenge is not found or expired
     */
    public ChallengeMetadata consumeChallenge(String challengeBase64) {
        String key = KEY_PREFIX + challengeBase64;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            throw new PasskeyAuthenticationException(ERROR_CHALLENGE_NOT_FOUND);
        }
        return deserializeMetadata(value);
    }

    /**
     * Serializes metadata to a delimited string.
     *
     * @param metadata the metadata to serialize
     * @return the serialized string
     */
    private String serializeMetadata(ChallengeMetadata metadata) {
        String userIdStr = (metadata.userId() != null) ? metadata.userId().toString() : "";
        return userIdStr + METADATA_DELIMITER + metadata.tenantId() + METADATA_DELIMITER + metadata.operation();
    }

    /**
     * Deserializes metadata from a delimited string.
     *
     * @param value the serialized string
     * @return the deserialized metadata
     */
    private ChallengeMetadata deserializeMetadata(String value) {
        String[] parts = value.split("\\" + METADATA_DELIMITER, METADATA_PARTS_COUNT);
        Long userId = parts[0].isEmpty() ? null : Long.valueOf(parts[0]);
        Long tenantId = Long.valueOf(parts[1]);
        String operation = parts[2];
        return new ChallengeMetadata(userId, tenantId, operation);
    }

    /**
     * Stores a raw JSON value for a challenge key (e.g., serialized creation/assertion options).
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @param optionsJson     the serialized options JSON
     */
    public void storeOptions(String challengeBase64, String optionsJson) {
        String key = KEY_PREFIX + "options:" + challengeBase64;
        redisTemplate.opsForValue().set(key, optionsJson, Duration.ofSeconds(properties.getChallengeTtlSeconds()));
    }

    /**
     * Retrieves and removes the stored options JSON for a challenge.
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @return the options JSON
     * @throws PasskeyAuthenticationException if not found or expired
     */
    public String consumeOptions(String challengeBase64) {
        String key = KEY_PREFIX + "options:" + challengeBase64;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            throw new PasskeyAuthenticationException(ERROR_CHALLENGE_NOT_FOUND);
        }
        return value;
    }

    /**
     * Challenge metadata stored alongside the challenge value.
     *
     * @param userId    the user ID (null for login — user unknown before assertion)
     * @param tenantId  the tenant ID from the request
     * @param operation the operation type (REGISTER or LOGIN)
     */
    public record ChallengeMetadata(Long userId, Long tenantId, String operation) {}
}
