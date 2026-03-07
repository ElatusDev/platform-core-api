/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.usecases.NonceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory nonce store for testing and local development.
 *
 * <p>Uses a {@link ConcurrentHashMap} with lazy expiry checks. Not suitable
 * for production multi-instance deployments (nonces are not shared between instances).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@ConditionalOnMissingBean(RedisNonceStore.class)
public class InMemoryNonceStore implements NonceStore {

    private final ConcurrentHashMap<String, Long> nonceMap = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String nonce) {
        Long expiry = nonceMap.get(nonce);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            nonceMap.remove(nonce);
            return false;
        }
        return true;
    }

    @Override
    public void store(String nonce, long ttlSeconds) {
        long expiryMillis = System.currentTimeMillis() + (ttlSeconds * 1000L);
        nonceMap.put(nonce, expiryMillis);
    }
}
