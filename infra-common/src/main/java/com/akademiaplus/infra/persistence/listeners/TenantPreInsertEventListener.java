package com.akademiaplus.infra.persistence.listeners;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Hibernate event listener that automatically assigns tenant ID to tenant-scoped entities before insert.
 * <p>
 * This listener is automatically triggered before any entity insertion and ensures
 * that entities implementing TenantScoped have the correct tenant ID set based on
 * the current security context.
 * <p>
 * The listener only applies to entities that implement the TenantScoped interface,
 * allowing non-tenant entities to be inserted without tenant assignment.
 */
@Slf4j
@Component
public class TenantPreInsertEventListener implements PreInsertEventListener {

    // Property names
    private static final String TENANT_ID_PROPERTY = "tenantId";

    // Error messages (public for testing)
    public static final String ERROR_TENANT_MISSING = "Missing tenant!";

    // Log messages (private - internal only)
    private static final String DEBUG_PRE_INSERT_EVENT = "PRE_INSERT event for: {}";
    private static final String DEBUG_TENANT_SET = "Set tenant {} for new entity {}";

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
    public TenantPreInsertEventListener(ObjectProvider<@NonNull TenantContextHolder> tenantContextHolderProvider) {
        this.tenantContextHolderProvider = tenantContextHolderProvider;
    }

    /**
     * Called by Hibernate before any entity insertion operation.
     * Automatically assigns tenant ID to tenant-scoped entities based on current security context.
     * <p>
     * The method performs the following operations:
     * 1. Checks if the entity implements TenantScoped interface
     * 2. Retrieves the current tenant context holder from the provider
     * 3. Gets the current tenant ID from the security context
     * 4. Sets the tenant ID on the entity
     * 5. Updates the Hibernate state to reflect the tenant ID
     * <p>
     * Non-tenant entities (those not implementing TenantScoped) are allowed to proceed
     * without tenant assignment.
     *
     * @param event The pre-insert event containing the entity being inserted and related context
     * @return false to allow the insert operation to continue, true would veto the operation
     * @throws InvalidTenantException if tenant context is missing
     */
    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        log.debug(DEBUG_PRE_INSERT_EVENT, event.getEntity().getClass().getSimpleName());

        if (event.getEntity() instanceof TenantScoped tenantEntity) {
            TenantContextHolder holder = tenantContextHolderProvider.getObject();
            Integer tenantId = holder.getTenantId()
                    .orElseThrow(() -> new InvalidTenantException(ERROR_TENANT_MISSING));

            tenantEntity.setTenantId(tenantId);
            updateTenantIdInState(event.getPersister(), event.getState(), tenantId);

            log.debug(DEBUG_TENANT_SET, tenantId, event.getEntity().getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Updates the Hibernate state array to reflect the tenant ID assignment.
     * Finds the tenantId property in the entity's property array and sets its value.
     *
     * @param persister The entity persister containing property metadata
     * @param state The state array containing entity property values
     * @param tenantId The tenant ID to assign
     */
    private void updateTenantIdInState(EntityPersister persister, Object[] state, Integer tenantId) {
        String[] propertyNames = persister.getPropertyNames();
        for (int i = 0; i < propertyNames.length; i++) {
            if (TENANT_ID_PROPERTY.equals(propertyNames[i])) {
                state[i] = tenantId;
                break;
            }
        }
    }
}