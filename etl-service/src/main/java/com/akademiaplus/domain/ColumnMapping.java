/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Maps a source column from the uploaded document to a target field in the platform schema.
 *
 * @param source    the original column name from the uploaded file
 * @param target    the target field name in the platform entity (may use "+" for compound targets like "firstName+lastName")
 * @param transform the transformation to apply during mapping
 */
public record ColumnMapping(String source, String target, TransformType transform) {
}
