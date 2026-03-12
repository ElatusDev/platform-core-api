/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import com.akademiaplus.domain.TransformType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Executes column value transformations during the mapping phase.
 *
 * <p>Each transform takes a raw cell value and produces one or more target field values.
 * The result is returned as a map because some transforms (e.g., SPLIT_NAME)
 * produce multiple target fields from a single source column.</p>
 */
@Component
public class TransformExecutor {

    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String ERROR_DATE_FROM_AGE = "Cannot convert non-integer to date: ";

    /**
     * Applies a transform to a raw value, producing one or more mapped field values.
     *
     * @param value     the raw cell value
     * @param transform the transform type to apply
     * @param target    the target field name (used for single-value transforms)
     * @return map of target field name to transformed value
     */
    public Map<String, String> apply(String value, TransformType transform, String target) {
        if (value == null) {
            value = "";
        }

        return switch (transform) {
            case NONE -> Map.of(target, value);
            case SPLIT_NAME -> splitName(value);
            case NORMALIZE_PHONE -> Map.of(target, normalizePhone(value));
            case DATE_FROM_AGE -> Map.of(target, dateFromAge(value));
            case UPPERCASE -> Map.of(target, value.toUpperCase());
            case LOWERCASE -> Map.of(target, value.toLowerCase());
            case TRIM -> Map.of(target, value.trim());
        };
    }

    private Map<String, String> splitName(String fullName) {
        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return Map.of(FIELD_FIRST_NAME, trimmed, FIELD_LAST_NAME, "");
        }
        String firstName = trimmed.substring(0, lastSpace).trim();
        String lastName = trimmed.substring(lastSpace + 1).trim();
        return Map.of(FIELD_FIRST_NAME, firstName, FIELD_LAST_NAME, lastName);
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            return phone;
        }
        return digits;
    }

    private String dateFromAge(String ageString) {
        try {
            int age = Integer.parseInt(ageString.trim());
            LocalDate approximateBirthDate = LocalDate.now().minusYears(age);
            return approximateBirthDate.toString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_DATE_FROM_AGE + ageString, e);
        }
    }
}
