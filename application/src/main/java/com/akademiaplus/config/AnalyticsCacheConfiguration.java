/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Redis cache configuration for analytics endpoints.
 * All analytics caches use a 5-minute TTL.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class AnalyticsCacheConfiguration {

    public static final Duration ANALYTICS_TTL = Duration.ofMinutes(5);

    public static final String CACHE_OVERVIEW = "analytics:overview";
    public static final String CACHE_STUDENTS = "analytics:students";
    public static final String CACHE_COURSES = "analytics:courses";
    public static final String CACHE_STAFF = "analytics:staff";
    public static final String CACHE_REVENUE = "analytics:revenue";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration analyticsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ANALYTICS_TTL)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
                .withInitialCacheConfigurations(Map.of(
                        CACHE_OVERVIEW, analyticsConfig,
                        CACHE_STUDENTS, analyticsConfig,
                        CACHE_COURSES, analyticsConfig,
                        CACHE_STAFF, analyticsConfig,
                        CACHE_REVENUE, analyticsConfig
                ))
                .build();
    }
}
