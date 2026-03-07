/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for access token session management.
 *
 * <p>Provides a {@link RedisTemplate} configured with string serializers
 * for storing JWT session metadata (jti to userId+tenantId mappings).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a RedisTemplate with string key and value serializers.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured RedisTemplate for session storage
     */
    @Bean
    public RedisTemplate<String, String> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
