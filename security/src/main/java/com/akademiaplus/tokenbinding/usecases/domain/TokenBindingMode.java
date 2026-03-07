/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Defines the strictness mode for token-to-device binding verification.
 *
 * @author ElatusDev
 * @since 1.0
 */
public enum TokenBindingMode {
    /** Full match required: IP + device fingerprint. */
    STRICT,
    /** Allow IP changes, reject device changes. */
    RELAXED,
    /** Token binding disabled — no fingerprint verification. */
    OFF
}
