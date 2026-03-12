/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Column value transformation types applied during the mapping phase.
 */
public enum TransformType {
    NONE,
    SPLIT_NAME,
    NORMALIZE_PHONE,
    DATE_FROM_AGE,
    UPPERCASE,
    LOWERCASE,
    TRIM
}
