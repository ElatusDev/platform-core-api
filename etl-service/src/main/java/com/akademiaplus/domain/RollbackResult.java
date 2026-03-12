/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Result of a migration rollback operation.
 *
 * @param jobId      the migration job identifier
 * @param rolledBack number of rows successfully rolled back
 * @param skipped    number of rows skipped (already deleted or error)
 */
public record RollbackResult(String jobId, int rolledBack, int skipped) {
}
