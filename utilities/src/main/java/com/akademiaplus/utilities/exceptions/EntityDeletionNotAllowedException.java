/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when an entity cannot be deleted due to either a database
 * constraint violation or a business rule.
 * <p>
 * Two construction paths:
 * <ul>
 *   <li>DB constraint: {@code new EntityDeletionNotAllowedException(type, id, cause)}
 *       — {@code reason} will be {@code null}</li>
 *   <li>Business rule: {@code new EntityDeletionNotAllowedException(type, id, reason)}
 *       — {@code reason} describes why deletion is blocked</li>
 * </ul>
 * Handled by {@code BaseControllerAdvice} → HTTP 409 Conflict.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class EntityDeletionNotAllowedException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final String reason;

    /**
     * DB constraint violation — the cause is the original
     * {@link org.springframework.dao.DataIntegrityViolationException}.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID whose deletion was attempted
     * @param cause      the database exception
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, Throwable cause) {
        super("Deletion of " + entityType + " with ID " + entityId + " not allowed", cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = null;
    }

    /**
     * Business rule violation — a human-readable reason explains why.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID whose deletion was attempted
     * @param reason     business rule description
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, String reason) {
        super("Deletion of " + entityType + " with ID " + entityId + " not allowed: " + reason);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = reason;
    }

    /** Returns the message property key identifying the entity type. */
    public String getEntityType() { return entityType; }

    /** Returns the entity ID whose deletion was attempted. */
    public String getEntityId() { return entityId; }

    /** Returns the business rule reason, or {@code null} for DB constraint violations. */
    public String getReason() { return reason; }
}
