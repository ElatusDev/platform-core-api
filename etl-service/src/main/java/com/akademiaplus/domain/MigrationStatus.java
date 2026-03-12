/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Lifecycle states for a {@link MigrationJob}.
 *
 * <p>State machine transitions:
 * UPLOADED → PARSED → ANALYZING → ANALYZED → MAPPING → VALIDATED → LOADING → COMPLETED
 * Any state may transition to FAILED on unrecoverable errors.</p>
 */
public enum MigrationStatus {
    UPLOADED,
    PARSED,
    ANALYZING,
    ANALYZED,
    MAPPING,
    VALIDATED,
    LOADING,
    COMPLETED,
    FAILED
}
