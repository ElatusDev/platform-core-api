/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.usecases.NonceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed nonce store for replay attack prevention.
 *
 * <p>Uses Redis SET with TTL to automatically expire nonces after the
 * configured window. The key format is {@code hmac:nonce:<nonce-value>}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisNonceStore implements NonceStore {

    /** Redis key prefix for nonce entries. */
    public static final String NONCE_KEY_PREFIX = "hmac:nonce:";

    /** Placeholder value stored in Redis. */
    public static final String NONCE_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs the nonce store with a Redis template.
     *
     * @param redisTemplate the Redis template
     */
    public RedisNonceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean exists(String nonce) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(NONCE_KEY_PREFIX + nonce));
    }

    @Override
    public void store(String nonce, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                NONCE_KEY_PREFIX + nonce,
                NONCE_VALUE,
                ttlSeconds,
                TimeUnit.SECONDS);
    }
}
