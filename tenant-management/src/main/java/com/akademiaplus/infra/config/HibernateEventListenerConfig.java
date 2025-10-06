package com.akademiaplus.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class HibernateEventListenerConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(ApplicationContext applicationContext) {
        return (Map<String, Object> hibernateProperties) -> {
            TenantEventIntegrator integrator = new TenantEventIntegrator(applicationContext);

            hibernateProperties.put(
                    "hibernate.integrator_provider",
                    (org.hibernate.jpa.boot.spi.IntegratorProvider) () -> List.of(integrator)
            );


            log.info("Configured Hibernate 6.x with tenant event integrator");
        };
    }


}