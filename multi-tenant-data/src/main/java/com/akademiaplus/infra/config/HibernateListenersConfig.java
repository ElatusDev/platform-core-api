package com.akademiaplus.infra.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Configuration class responsible for registering custom Hibernate event listeners
 * to support multi-tenant operations in the application.
 * <p>
 * This class automatically registers tenant-aware event listeners that intercept
 * database operations (insert, update, delete) to ensure proper tenant isolation
 * and automatic tenant ID injection for all entity operations.
 * <p>
 * The listeners are registered at application startup and will be triggered
 * automatically by Hibernate for all entity lifecycle events.
 */
@Component
public class HibernateListenersConfig implements ApplicationContextAware {

    /**
     * Hibernate's EntityManagerFactory used to access the underlying SessionFactory
     * and register custom event listeners with Hibernate's event system.
     */
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Spring's application context used to retrieve bean instances of the
     * custom event listeners that need to be registered with Hibernate.
     */
    private ApplicationContext applicationContext;

    /**
     * Constructor injection of the EntityManagerFactory.
     *
     * @param entityManagerFactory The JPA EntityManagerFactory that wraps Hibernate's SessionFactory
     */
    public HibernateListenersConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Registers custom tenant-aware event listeners with Hibernate's event system.
     * This method is called automatically after Spring has fully initialized this bean
     * and all its dependencies are satisfied.
     * <p>
     * The method performs the following operations:
     * 1. Unwraps the Hibernate SessionFactory from the JPA EntityManagerFactory
     * 2. Retrieves Hibernate's EventListenerRegistry
     * 3. Gets instances of custom event listeners from Spring context
     * 4. Registers each listener with the appropriate Hibernate event types
     *
     * @throws IllegalStateException if any required Hibernate components cannot be accessed
     */
    @PostConstruct
    public void registerListeners() {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("EntityManagerFactory is not initialized.");
        }

        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        if (sessionFactory == null) {
            throw new IllegalStateException("Could not unwrap SessionFactory from EntityManagerFactory.");
        }

        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        if (registry == null) {
            throw new IllegalStateException("Could not get EventListenerRegistry from SessionFactory.");
        }

        TenantInsertEventListener insertListener = applicationContext.getBean(TenantInsertEventListener.class);
        TenantUpdateEventListener updateListener = applicationContext.getBean(TenantUpdateEventListener.class);
        TenantDeleteEventListener deleteListener = applicationContext.getBean(TenantDeleteEventListener.class);

        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(insertListener);
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(updateListener);
        registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(deleteListener);
    }

    /**
     * Callback method implemented from ApplicationContextAware interface.
     * Spring calls this method to provide access to the application context,
     * which is needed to retrieve the custom event listener beans.
     * <p>
     * This method is called during Spring's bean initialization phase,
     * before the @PostConstruct method is executed.
     *
     * @param applicationContext The Spring application context containing all managed beans
     * @throws BeansException if the application context cannot be set
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}