/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Abstract base class providing audit functionality for platform entities.
 * Automatically tracks creation and modification timestamps for compliance and debugging.
 * <p>
 * This class should be extended by entities that require audit trail capabilities.
 * All timestamp management is handled automatically through JPA lifecycle methods.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class Auditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Timestamp when the record was created in the system.
     * Automatically set on entity creation for audit purposes.
     * <p>
     * This represents the system creation time, which may differ from business dates.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated in the system.
     * Automatically updated on entity modification for audit purposes.
     * <p>
     * Used for tracking data changes and debugging issues.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle method called before persisting the entity.
     * Automatically sets audit timestamps for new records.
     */
    @PrePersist
    protected void onAuditPrePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA lifecycle method called before updating the entity.
     * Automatically updates the last modified timestamp.
     */
    @PreUpdate
    protected void onAuditPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}