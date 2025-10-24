/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.persistence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Abstract base class providing soft delete functionality for platform entities.
 * Enables logical deletion without permanent data loss for audit and recovery purposes.
 * <p>
 * Soft deleted records are filtered out from normal queries but remain in the database
 * for audit trails, compliance, and potential data recovery scenarios.
 * <p>
 * IMPORTANT: Entity classes extending this class MUST add:
 * {@code @SQLDelete(sql = "UPDATE [table_name] SET deleted_at = CURRENT_TIMESTAMP WHERE [id_column] = ?")}
 * to enforce soft delete at the Hibernate level.
 * <p>
 * This implementation uses Hibernate 6.3+ annotations.
 */
@Getter
@Setter
@MappedSuperclass
@FilterDef(name = "softDeleteFilter", defaultCondition = "deleted_at IS NULL")
@Filter(name = "softDeleteFilter", condition = "deleted_at IS NULL")
@SQLRestriction("deleted_at IS NULL")
public abstract class SoftDeletable extends Auditable {

    /**
     * Timestamp when the record was soft deleted.
     * Null indicates the record is active and not deleted.
     * <p>
     * Used for audit trail and potential data recovery without permanent deletion.
     * Records with this field set are excluded from normal business operations.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Marks the entity as soft deleted by setting the deletion timestamp.
     * The record remains in the database but is excluded from normal queries.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft deleted entity by clearing the deletion timestamp.
     * The record becomes available for normal business operations again.
     * <p>
     * NOTE: This method should only be used by the separate data recovery application,
     * not by the platform-core-api.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Checks if the entity is currently soft deleted.
     * @return true if the entity is soft deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Checks if the entity is currently active (not soft deleted).
     * @return true if the entity is active, false if soft deleted
     */
    public boolean isActive() {
        return this.deletedAt == null;
    }

    /**
     * Hook method called before soft delete.
     * Can be overridden by subclasses to perform cleanup or validation.
     */
    @PreRemove
    protected void onPreRemove() {
        // Subclasses can override this to perform actions before soft delete
        // For example: validate deletion permissions, cascade soft deletes, etc.
    }

    /**
     * Hook method called after entity is loaded.
     * Ensures soft-deleted entities are not accidentally loaded.
     */
    @PostLoad
    protected void onPostLoad() {
        // Additional safety check - this should never be called for soft-deleted entities
        // due to @SQLRestriction, but provides an extra layer of protection
        if (isDeleted()) {
            throw new IllegalStateException("Soft-deleted entity should not be loaded: " + this.getClass().getSimpleName());
        }
    }
}