package com.akademiaplus.infra.persistence.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * Hibernate integrator that registers custom event listeners into Hibernate's event system.
 * <p>
 * This integrator is responsible for retrieving event listener beans from the Spring
 * application context and registering them with Hibernate during SessionFactory initialization.
 * It registers the following event listeners:
 * <ul>
 *   <li>Tenant pre-insert listener - assigns tenant ID to new entities</li>
 *   <li>Tenant pre-update listener - validates tenant context during updates</li>
 *   <li>Tenant pre-delete listener - validates tenant context during deletes</li>
 *   <li>ID generation listener - assigns generated IDs to entities</li>
 * </ul>
 */
@Slf4j
public class TenantEventIntegrator implements Integrator {

    // Spring bean names
    private static final String BEAN_TENANT_PRE_INSERT_LISTENER = "tenantPreInsertEventListener";
    private static final String BEAN_TENANT_PRE_UPDATE_LISTENER = "tenantPreUpdateEventListener";
    private static final String BEAN_TENANT_PRE_DELETE_LISTENER = "tenantPreDeleteEventListener";
    private static final String BEAN_ID_GENERATION_LISTENER = "idAssignationPreInsertEventListener";

    // Error messages (public for testing)
    public static final String ERROR_APPLICATION_CONTEXT_NOT_AVAILABLE = "ApplicationContext not available!";

    // Log messages (private - internal only)
    private static final String INFO_REGISTERING_LISTENERS = "🚀 Registering tenant event listeners in Hibernate 6.x";
    private static final String INFO_LISTENERS_REGISTERED = "✅ Tenant event listeners registered successfully";

    private final ApplicationContext applicationContext;

    /**
     * Constructor for dependency injection.
     *
     * @param applicationContext The Spring application context used to retrieve event listener beans
     */
    public TenantEventIntegrator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Integrates custom event listeners into Hibernate's event system.
     * <p>
     * This method is called by Hibernate during SessionFactory initialization.
     * It retrieves event listener beans from the Spring context and registers them
     * with Hibernate's EventListenerRegistry for the appropriate event types.
     *
     * @param metadata The Hibernate metadata
     * @param bootstrapContext The bootstrap context
     * @param sessionFactory The session factory implementation
     */
    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {

        log.info(INFO_REGISTERING_LISTENERS);

        if (applicationContext == null) {
            log.error(ERROR_APPLICATION_CONTEXT_NOT_AVAILABLE);
            return;
        }

        EventListenerRegistry eventListenerRegistry =
                sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        // Get listeners from Spring context
        PreInsertEventListener tenantInsertListener = applicationContext.getBean(
                BEAN_TENANT_PRE_INSERT_LISTENER,
                PreInsertEventListener.class
        );
        PreUpdateEventListener tenantUpdateListener = applicationContext.getBean(
                BEAN_TENANT_PRE_UPDATE_LISTENER,
                PreUpdateEventListener.class
        );
        PreDeleteEventListener tenantDeleteListener = applicationContext.getBean(
                BEAN_TENANT_PRE_DELETE_LISTENER,
                PreDeleteEventListener.class
        );
        PreInsertEventListener idGenerationListener = applicationContext.getBean(
                BEAN_ID_GENERATION_LISTENER,
                PreInsertEventListener.class
        );

        // Register listeners with Hibernate
        Objects.requireNonNull(eventListenerRegistry, "EventListenerRegistry must not be null")
                .appendListeners(EventType.PRE_INSERT, tenantInsertListener);
        eventListenerRegistry.appendListeners(EventType.PRE_UPDATE, tenantUpdateListener);
        eventListenerRegistry.appendListeners(EventType.PRE_DELETE, tenantDeleteListener);
        eventListenerRegistry.appendListeners(EventType.PRE_INSERT, idGenerationListener);

        log.info(INFO_LISTENERS_REGISTERED);
    }

    /**
     * Called when the SessionFactory is being closed.
     * <p>
     * Currently no cleanup is required, but this method can be used for
     * resource cleanup if needed in the future.
     *
     * @param sessionFactory The session factory implementation
     * @param serviceRegistry The service registry
     */
    @Override
    public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        // No cleanup required
    }
}