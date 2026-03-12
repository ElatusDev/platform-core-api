/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Describes a validation failure for a specific field in a {@link MigrationRow}.
 *
 * @param field    the field name that failed validation
 * @param message  human-readable error description
 * @param severity the error severity (ERROR or WARNING)
 */
public record ValidationError(String field, String message, String severity) {
}
