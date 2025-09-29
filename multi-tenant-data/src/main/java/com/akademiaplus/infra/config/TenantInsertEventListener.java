package com.akademiaplus.infra.config;

import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.infra.TenantContextHolder;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Hibernate event listener that automatically assigns tenant IDs during entity insert operations.
 * <p>
 * This listener is automatically triggered before any entity insertion and ensures
 * that tenant-scoped entities have the correct tenant ID set based on the current
 * security context. This provides automatic tenant isolation without requiring
 * manual tenant ID assignment in application code.
 * <p>
 * The listener only applies to entities that implement the TenantScoped interface,
 * allowing non-tenant entities to be inserted without tenant processing.
 */
@Component
public class TenantInsertEventListener implements PreInsertEventListener {

    /**
     * Provider for the TenantContextHolder to handle request-scoped bean injection.
     * Using ObjectProvider allows safe access to request-scoped beans from singleton components.
     */
    private final ObjectProvider<TenantContextHolder> tenantContextHolderProvider;

    /**
     * Constructor for dependency injection of the tenant context holder provider.
     *
     * @param tenantContextHolderProvider The provider that supplies TenantContextHolder instances
     */
    public TenantInsertEventListener(ObjectProvider<TenantContextHolder> tenantContextHolderProvider) {
        this.tenantContextHolderProvider = tenantContextHolderProvider;
    }

    /**
     * Called by Hibernate before any entity insertion operation.
     * Automatically assigns the tenant ID to tenant-scoped entities if not already set.
     * <p>
     * The method performs the following operations:
     * 1. Checks if the entity implements TenantScoped interface
     * 2. Verifies if the entity's tenant ID is null (not already set)
     * 3. Retrieves the current tenant context holder from the provider
     * 4. Gets the current tenant ID from the security context
     * 5. Assigns the context tenant ID to the entity
     * <p>
     * Non-tenant entities (those not implementing TenantScoped) are allowed to proceed
     * without tenant processing. Entities that already have a tenant ID assigned
     * are left unchanged to preserve explicit tenant assignments.
     *
     * @param event The pre-insert event containing the entity being inserted and related context
     * @return false to allow the insert operation to continue, true would veto the operation
     */
    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof TenantScoped tenantAwareEntity) {
            if (tenantAwareEntity.getTenantId() == null) {
                TenantContextHolder tenantContextHolder = tenantContextHolderProvider.getObject();
                Integer contextTenantId = tenantContextHolder.getTenantId();
                tenantAwareEntity.setTenantId(contextTenantId);
            }
        }

        return false;
    }
}