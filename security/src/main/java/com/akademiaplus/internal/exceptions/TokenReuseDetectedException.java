/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.exceptions;

/**
 * Thrown when a previously consumed refresh token is reused, indicating
 * a potential token theft. All tokens in the family are revoked.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TokenReuseDetectedException extends RuntimeException {

    /** Error message template with family ID placeholder. */
    public static final String ERROR_MESSAGE = "Refresh token reuse detected for family %s — all tokens revoked";

    /**
     * Constructs a TokenReuseDetectedException for the given family.
     *
     * @param familyId the compromised token family ID
     */
    public TokenReuseDetectedException(String familyId) {
        super(String.format(ERROR_MESSAGE, familyId));
    }
}
