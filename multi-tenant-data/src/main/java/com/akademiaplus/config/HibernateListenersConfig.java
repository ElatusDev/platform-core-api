package com.akademiaplus.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class HibernateListenersConfig implements ApplicationContextAware {

    private final EntityManagerFactory entityManagerFactory;
    private ApplicationContext applicationContext;

    public HibernateListenersConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}