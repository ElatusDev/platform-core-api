/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.exceptions;

/**
 * Thrown when a magic link token has already been consumed.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class MagicLinkTokenAlreadyUsedException extends RuntimeException {

    /** Error message for already-used tokens. */
    public static final String ERROR_TOKEN_ALREADY_USED = "Magic link token has already been used";

    /** Error code for already-used tokens. */
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_ALREADY_USED";

    /**
     * Constructs a new exception with the default message.
     */
    public MagicLinkTokenAlreadyUsedException() {
        super(ERROR_TOKEN_ALREADY_USED);
    }
}
