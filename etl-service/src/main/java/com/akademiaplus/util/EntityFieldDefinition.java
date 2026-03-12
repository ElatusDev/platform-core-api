/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

/**
 * Describes a single field in a target entity schema for ETL mapping purposes.
 *
 * @param name        the field name as it appears in the target entity
 * @param type        the Java type name (String, Long, LocalDate, etc.)
 * @param required    whether the field is required (NOT NULL in DB)
 * @param description a brief human-readable description for Claude context
 */
public record EntityFieldDefinition(String name, String type, boolean required, String description) {
}
