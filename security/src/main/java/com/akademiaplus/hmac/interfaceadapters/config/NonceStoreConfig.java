/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters.config;

import com.akademiaplus.hmac.interfaceadapters.InMemoryNonceStore;
import com.akademiaplus.hmac.interfaceadapters.AkademiaPlusRedisNonceStore;
import com.akademiaplus.hmac.usecases.NonceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configures the {@link NonceStore} bean for HMAC replay-attack prevention.
 *
 * <p>Uses Redis when {@link StringRedisTemplate} is available; falls back to
 * an in-memory store otherwise (local dev / tests).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class NonceStoreConfig {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    NonceStore redisNonceStore(StringRedisTemplate redisTemplate) {
        return new AkademiaPlusRedisNonceStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(NonceStore.class)
    NonceStore inMemoryNonceStore() {
        return new InMemoryNonceStore();
    }
}
