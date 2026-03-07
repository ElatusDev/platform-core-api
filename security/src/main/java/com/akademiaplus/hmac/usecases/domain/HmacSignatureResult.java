/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases.domain;

/**
 * Result of an HMAC signature computation or verification.
 *
 * @param signature the computed HMAC-SHA256 hex string
 * @param valid     whether the signature matched the expected value (for verification)
 * @author ElatusDev
 * @since 1.0
 */
public record HmacSignatureResult(String signature, boolean valid) {

    /**
     * Creates a result for a newly computed signature (not yet verified).
     *
     * @param signature the computed signature
     * @return a new HmacSignatureResult
     */
    public static HmacSignatureResult computed(String signature) {
        return new HmacSignatureResult(signature, true);
    }
}
