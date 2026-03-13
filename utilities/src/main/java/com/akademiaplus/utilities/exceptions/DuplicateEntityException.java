/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a create or update operation violates a unique constraint
 * on a known field (email, phone number, etc.).
 * <p>
 * Handled by {@code BaseControllerAdvice} → HTTP 409 Conflict.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class DuplicateEntityException extends RuntimeException {

    /** Message template: {@code "Duplicate %s for %s"}. */
    public static final String MESSAGE_TEMPLATE = "Duplicate %s for %s";

    private final String entityType;
    private final String field;

    /**
     * Creates a new duplicate entity exception for application-level checks
     * where no underlying database exception exists.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param field      the field that has a duplicate value (e.g., "email", "phoneNumber")
     */
    public DuplicateEntityException(String entityType, String field) {
        super(String.format(MESSAGE_TEMPLATE, field, entityType));
        this.entityType = entityType;
        this.field = field;
    }

    /**
     * Creates a new duplicate entity exception.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param field      the field that has a duplicate value (e.g., "email", "phoneNumber")
     * @param cause      the original {@link org.springframework.dao.DataIntegrityViolationException}
     */
    public DuplicateEntityException(String entityType, String field, Throwable cause) {
        super(String.format(MESSAGE_TEMPLATE, field, entityType), cause);
        this.entityType = entityType;
        this.field = field;
    }

    /** Returns the message property key identifying the entity type. */
    public String getEntityType() { return entityType; }

    /** Returns the field name that has a duplicate value. */
    public String getField() { return field; }
}
