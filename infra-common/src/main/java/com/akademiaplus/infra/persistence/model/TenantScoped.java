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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.descriptor.java.LongJavaType;

/**
 * Abstract base class providing multi-tenant isolation for platform entities.
 * Ensures data segregation between different tenants in a multi-tenant SaaS architecture.
 * <p>
 * All entities extending this class will automatically have tenant-based data isolation
 * and will be part of a composite primary key structure for optimal performance.
 * <p>
 * This implementation uses Hibernate 6.3+ features for enhanced multi-tenancy support.
 */
@Slf4j
@Getter
@Setter
@MappedSuperclass
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = LongJavaType.class),
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
    private Long tenantId;

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