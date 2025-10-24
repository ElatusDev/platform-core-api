package com.akademiaplus.infra.persistence.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for registering custom Hibernate event listeners.
 * <p>
 * This configuration integrates tenant-aware event listeners into Hibernate's
 * event system by providing a custom integrator. The integrator is responsible
 * for registering all tenant-scoped event listeners during Hibernate initialization.
 */
@Slf4j
@Configuration
public class HibernateEventListenerConfig {

    // Hibernate property keys
    private static final String HIBERNATE_INTEGRATOR_PROVIDER_PROPERTY = "hibernate.integrator_provider";

    // Log messages (private - internal only)
    private static final String INFO_INTEGRATOR_CONFIGURED = "Configured Hibernate 6.x with tenant event integrator";

    /**
     * Creates a HibernatePropertiesCustomizer bean that registers the tenant event integrator.
     * <p>
     * The customizer adds the TenantEventIntegrator to Hibernate's configuration,
     * which ensures all tenant-scoped event listeners are properly registered
     * with Hibernate's event system during application startup.
     *
     * @param applicationContext The Spring application context used to retrieve event listener beans
     * @return A customizer that configures Hibernate with the tenant event integrator
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ApplicationContext applicationContext) {
        return (Map<String, Object> hibernateProperties) -> {
            TenantEventIntegrator integrator = new TenantEventIntegrator(applicationContext);

            hibernateProperties.put(
                    HIBERNATE_INTEGRATOR_PROVIDER_PROPERTY,
                    (IntegratorProvider) () -> List.of(integrator)
            );

            log.info(INFO_INTEGRATOR_CONFIGURED);
        };
    }
}