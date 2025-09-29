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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.descriptor.java.IntegerJavaType;

/**
 * Abstract base class providing multi-tenant isolation for platform entities.
 * Ensures data segregation between different tenants in a multi-tenant SaaS architecture.
 * <p>
 * All entities extending this class will automatically have tenant-based data isolation
 * and will be part of a composite primary key structure for optimal performance.
 * <p>
 * This implementation uses Hibernate 6.3+ features for enhanced multi-tenancy support.
 */
@Getter
@Setter
@MappedSuperclass
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = IntegerJavaType.class),
        defaultCondition = "tenant_id = :tenantId"
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantScoped extends SoftDeletable {

    /**
     * Tenant identifier for multi-tenant isolation.
     * Part of composite primary key for all tenant-scoped entities.
     * <p>
     * Ensures complete data segregation between different organizational tenants.
     * This field is immutable once set to maintain data integrity.
     * <p>
     * Uses @TenantId annotation for Hibernate 6.2+ native multi-tenancy support.
     */
    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Integer tenantId;

    /**
     * Sets the tenant ID for this entity.
     * Should only be called once during entity creation.
     * <p>
     * @param tenantId The tenant identifier to associate with this entity
     * @throws IllegalStateException if tenant ID is already assigned
     */
    public void assignToTenant(Integer tenantId) {
        if (this.tenantId != null) {
            throw new IllegalStateException("Tenant ID cannot be changed once assigned");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        this.tenantId = tenantId;
    }

    /**
     * Checks if this entity belongs to the specified tenant.
     * <p>
     * @param tenantId The tenant ID to check against
     * @return true if the entity belongs to the specified tenant
     */
    public boolean belongsToTenant(Integer tenantId) {
        return this.tenantId != null && this.tenantId.equals(tenantId);
    }

    /**
     * Validates that the current entity belongs to the specified tenant.
     * Throws exception if validation fails.
     * <p>
     * @param tenantId The tenant ID to validate against
     * @throws SecurityException if the entity doesn't belong to the specified tenant
     */
    public void validateTenantAccess(Integer tenantId) {
        if (!belongsToTenant(tenantId)) {
            throw new SecurityException(
                    String.format("Access denied: Entity belongs to tenant %d, but tenant %d was requested",
                            this.tenantId, tenantId)
            );
        }
    }

    /**
     * Hook method called before persist to ensure tenant is set.
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.tenantId == null) {
            throw new IllegalStateException("Tenant ID must be set before persisting entity");
        }
    }

    /**
     * Hook method called after load to validate tenant isolation.
     * Provides an additional security layer for multi-tenant data isolation.
     */
    @PostLoad
    protected void onPostLoadTenantCheck() {
        super.onPostLoad(); // Call parent soft-delete check

        // Additional validation can be added here if needed
        // For example, checking against a ThreadLocal tenant context
    }
}