/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a tenant-scoped entity cannot be found by its composite key.
 * <p>
 * The {@code entityType} field holds a message property key
 * (e.g., {@link com.akademiaplus.utilities.EntityType#EMPLOYEE})
 * that resolves to a localized display name via {@code MessageService}.
 * <p>
 * Handled by {@code BaseControllerAdvice} → HTTP 404 Not Found.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    /**
     * Creates a new entity not found exception.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID that was not found (as String for display)
     */
    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " with ID " + entityId + " not found");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /** Returns the message property key identifying the entity type. */
    public String getEntityType() { return entityType; }

    /** Returns the entity ID that was not found. */
    public String getEntityId() { return entityId; }
}
