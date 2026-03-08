/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.exceptions;

/**
 * Thrown when a magic link token has expired.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class MagicLinkTokenExpiredException extends RuntimeException {

    /** Error message for expired tokens. */
    public static final String ERROR_TOKEN_EXPIRED = "Magic link token has expired";

    /** Error code for expired tokens. */
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_EXPIRED";

    /**
     * Constructs a new exception with the default message.
     */
    public MagicLinkTokenExpiredException() {
        super(ERROR_TOKEN_EXPIRED);
    }
}
