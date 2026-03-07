/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

/**
 * Stores and checks nonces to prevent replay attacks.
 *
 * <p>Implementations must ensure that a nonce can only be used once within
 * the configured TTL window (default 5 minutes).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface NonceStore {

    /**
     * Checks if the nonce has already been used.
     *
     * @param nonce the nonce to check
     * @return true if the nonce already exists (replay detected)
     */
    boolean exists(String nonce);

    /**
     * Stores the nonce with a TTL. After TTL expiry, the nonce may be reused.
     *
     * @param nonce      the nonce to store
     * @param ttlSeconds the time-to-live in seconds
     */
    void store(String nonce, long ttlSeconds);
}
