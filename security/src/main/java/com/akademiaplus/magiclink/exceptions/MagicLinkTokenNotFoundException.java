/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.exceptions;

/**
 * Thrown when a magic link token hash is not found in the database.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class MagicLinkTokenNotFoundException extends RuntimeException {

    /** Error message for token not found. */
    public static final String ERROR_TOKEN_NOT_FOUND = "Magic link token not found";

    /** Error code for token not found. */
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_NOT_FOUND";

    /**
     * Constructs a new exception with the default message.
     */
    public MagicLinkTokenNotFoundException() {
        super(ERROR_TOKEN_NOT_FOUND);
    }
}
