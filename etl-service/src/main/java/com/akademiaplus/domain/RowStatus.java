/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Lifecycle states for a {@link MigrationRow}.
 *
 * <p>State machine transitions:
 * RAW → MAPPED → VALID/INVALID → LOADED/LOAD_FAILED</p>
 */
public enum RowStatus {
    RAW,
    MAPPED,
    VALID,
    INVALID,
    LOADED,
    LOAD_FAILED
}
