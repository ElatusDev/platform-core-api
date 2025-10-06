package com.akademiaplus.infra.config;

import com.akademiaplus.infra.listeners.TenantPreInsertEventListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

@AllArgsConstructor
@Slf4j
public class TenantEventIntegrator implements Integrator {

    private ApplicationContext applicationContext;

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {

        log.info("🚀 Registering tenant event listeners in Hibernate 6.x");

        if (applicationContext == null) {
            log.error("ApplicationContext not available!");
            return;
        }

        final EventListenerRegistry eventListenerRegistry =
                sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        // Get listeners from Spring context
        var insertListener = (TenantPreInsertEventListener) applicationContext.getBean(
                "tenantPreInsertEventListener",
                org.hibernate.event.spi.PreInsertEventListener.class
        );
        var updateListener = applicationContext.getBean(
                "tenantPreUpdateEventListener",
                org.hibernate.event.spi.PreUpdateEventListener.class
        );
        var deleteListener = applicationContext.getBean(
                "tenantPreDeleteEventListener",
                org.hibernate.event.spi.PreDeleteEventListener.class
        );

        var idGeneratorListener = applicationContext.getBean(
                "idGenerationPreInsertEventListener",
                org.hibernate.event.spi.PreInsertEventListener.class);

        Objects.requireNonNull(eventListenerRegistry)
                .appendListeners(EventType.PRE_INSERT, insertListener);
        eventListenerRegistry.appendListeners(EventType.PRE_UPDATE, updateListener);
        eventListenerRegistry.appendListeners(EventType.PRE_DELETE, deleteListener);
        eventListenerRegistry.appendListeners(EventType.PRE_INSERT, idGeneratorListener);

        log.info("✅ Tenant event listeners registered successfully");
    }

    @Override
    public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        // Cleanup if needed
    }
}