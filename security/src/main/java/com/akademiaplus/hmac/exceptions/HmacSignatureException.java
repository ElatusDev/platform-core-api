/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.exceptions;

/**
 * Thrown when HMAC signature verification fails.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class HmacSignatureException extends RuntimeException {

    /** Error message for invalid HMAC signature. */
    public static final String ERROR_SIGNATURE_INVALID = "HMAC signature verification failed: %s";

    /** Error message for missing required HMAC headers. */
    public static final String ERROR_MISSING_HEADERS = "Missing required HMAC headers: %s";

    /** Error message for timestamp out of tolerance. */
    public static final String ERROR_TIMESTAMP_EXPIRED =
            "Request timestamp outside tolerance window: delta=%d seconds, max=%d seconds";

    /** Error message for replay attack (nonce reuse). */
    public static final String ERROR_NONCE_REPLAY = "Nonce has already been used (replay attack detected): %s";

    /** Error message for body hash mismatch. */
    public static final String ERROR_BODY_HASH_MISMATCH = "Request body hash does not match X-Body-Hash header";

    /** Error message for no signing key configured. */
    public static final String ERROR_NO_KEY_CONFIGURED = "No HMAC signing key configured for app: %s";

    /** Error code returned in the 401 response body. */
    public static final String ERROR_CODE_HMAC = "HMAC_SIGNATURE_INVALID";

    /**
     * Creates a new HmacSignatureException with the given message.
     *
     * @param message the detail message
     */
    public HmacSignatureException(String message) {
        super(message);
    }
}
