package com.akademiaplus.infra.persistence.listeners;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Hibernate event listener that enforces tenant isolation during entity update operations.
 * <p>
 * This listener is automatically triggered before any entity update and validates
 * that the entity being updated belongs to the same tenant as the current security context.
 * This prevents accidental or malicious cross-tenant data modification during update operations.
 * <p>
 * The listener only applies to entities that implement the TenantScoped interface,
 * allowing non-tenant entities to be updated without tenant validation.
 */
@Slf4j
@Component
public class TenantPreUpdateEventListener implements PreUpdateEventListener {

    // Error messages (public for testing)
    public static final String ERROR_TENANT_MISSING = "Missing or invalid tenant!";
    public static final String ERROR_TENANT_MISMATCH = "Tenant mismatch. Cannot modify entity belonging to a different tenant.";

    // Log messages (private - internal only)
    private static final String WARN_TENANT_MISMATCH_ATTEMPT =
            "SECURITY WARNING: Attempted to update entity {} from tenant {} while in context of tenant {}";

    /**
     * Provider for the TenantContextHolder to handle request-scoped bean injection.
     * Using ObjectProvider allows safe access to request-scoped beans from singleton components.
     */
    private final ObjectProvider<@NonNull TenantContextHolder> tenantContextHolderProvider;

    /**
     * Constructor for dependency injection of the tenant context holder provider.
     *
     * @param tenantContextHolderProvider The provider that supplies TenantContextHolder instances
     */
    public TenantPreUpdateEventListener(ObjectProvider<@NonNull TenantContextHolder> tenantContextHolderProvider) {
        this.tenantContextHolderProvider = tenantContextHolderProvider;
    }

    /**
     * Called by Hibernate before any entity update operation.
     * Validates that tenant-scoped entities can only be updated within their own tenant context.
     * <p>
     * The method performs the following validation:
     * 1. Checks if the entity implements TenantScoped interface
     * 2. Retrieves the current tenant context holder from the provider
     * 3. Gets the current tenant ID from the security context
     * 4. Compares the entity's tenant ID with the current context tenant ID
     * 5. Throws SecurityException if there's a tenant mismatch
     * <p>
     * Non-tenant entities (those not implementing TenantScoped) are allowed to proceed
     * without tenant validation.
     *
     * @param event The pre-update event containing the entity being updated and related context
     * @return false to allow the update operation to continue, true would veto the operation
     * @throws InvalidTenantException if tenant context is missing
     * @throws SecurityException if the entity belongs to a different tenant than the current context
     */
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (event.getEntity() instanceof TenantScoped tenantAwareEntity) {
            TenantContextHolder tenantContextHolder = tenantContextHolderProvider.getObject();
            Long contextTenantId = tenantContextHolder.getTenantId()
                    .orElseThrow(() -> new InvalidTenantException(ERROR_TENANT_MISSING));

            if (!Objects.equals(contextTenantId, tenantAwareEntity.getTenantId())) {
                log.warn(WARN_TENANT_MISMATCH_ATTEMPT,
                        event.getEntity().getClass().getSimpleName(),
                        tenantAwareEntity.getTenantId(),
                        contextTenantId);
                throw new SecurityException(ERROR_TENANT_MISMATCH);
            }
        }
        return false;
    }
}