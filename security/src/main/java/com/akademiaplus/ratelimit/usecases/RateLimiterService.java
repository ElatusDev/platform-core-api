/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.usecases;

import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements sliding window rate limiting using Redis sorted sets.
 *
 * <p>Each request is added to a Redis sorted set with its timestamp as score.
 * Expired entries are pruned on each check, and the current count determines
 * whether the request is allowed.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class RateLimiterService {

    /** Prefix for per-IP rate limit keys. */
    public static final String KEY_PREFIX_IP = "rate:ip:";

    /** Prefix for per-user rate limit keys. */
    public static final String KEY_PREFIX_USER = "rate:user:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs a RateLimiterService with the given Redis template.
     *
     * @param redisTemplate the Redis template for sorted set operations
     */
    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks whether a request identified by the given key is within
     * the rate limit, and records the request if allowed.
     *
     * <p>Uses the sliding window algorithm:
     * <ol>
     *   <li>Remove entries older than {@code windowMs} from the sorted set</li>
     *   <li>Count remaining entries</li>
     *   <li>If count &lt; limit, add new entry and allow</li>
     *   <li>If count &gt;= limit, reject</li>
     * </ol>
     *
     * @param key      the rate limit key (e.g., "rate:ip:192.168.1.1:login")
     * @param limit    maximum requests allowed in the window
     * @param windowMs window duration in milliseconds
     * @return a {@link RateLimitResult} with the decision and header metadata
     */
    public RateLimitResult checkRateLimit(String key, int limit, long windowMs) {
        long nowMs = Instant.now().toEpochMilli();
        long windowStartMs = nowMs - windowMs;
        long resetEpochSeconds = (nowMs + windowMs) / 1000L;

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. Remove expired entries
        zSetOps.removeRangeByScore(key, 0, windowStartMs);

        // 2. Count current entries
        Long currentCount = zSetOps.zCard(key);
        long count = (currentCount != null) ? currentCount : 0L;

        if (count >= limit) {
            return new RateLimitResult(false, limit, 0, resetEpochSeconds);
        }

        // 3. Add new entry with timestamp as score, UUID as member for uniqueness
        String member = nowMs + ":" + UUID.randomUUID();
        zSetOps.add(key, member, nowMs);

        // 4. Set TTL to auto-expire the key after the window
        long ttlSeconds = TimeUnit.MILLISECONDS.toSeconds(windowMs) + 1L;
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

        int remaining = (int) (limit - count - 1);
        return new RateLimitResult(true, limit, Math.max(remaining, 0), resetEpochSeconds);
    }
}
