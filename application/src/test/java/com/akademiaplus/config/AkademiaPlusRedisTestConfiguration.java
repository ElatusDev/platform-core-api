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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that provides mock Redis infrastructure beans.
 *
 * <p>Integration tests run without a Redis instance. This configuration
 * provides mock {@link RedisConnectionFactory}, {@link StringRedisTemplate},
 * and {@link RedisTemplate} beans so that all Redis-dependent components
 * (filters, session store, nonce store, rate limiter, etc.) can be created
 * by the Spring context without a running Redis server.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@Profile("mock-data-service")
public class AkademiaPlusRedisTestConfiguration {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @Bean(name = "akademiaPlusRedisTemplate")
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> akademiaPlusRedisTemplate() {
        return mock(RedisTemplate.class);
    }
}
