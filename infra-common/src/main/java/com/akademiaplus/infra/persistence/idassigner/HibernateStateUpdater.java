package com.akademiaplus.infra.persistence.idassigner;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.springframework.stereotype.Component;

/**
 * Handles Hibernate-specific operations for updating event state
 * Isolates Hibernate integration concerns from the main ID assignment logic
 */
@Slf4j
@Component
public class HibernateStateUpdater {

    // Log messages (public for testing)
    public static final String WARN_PROPERTY_NOT_FOUND = "Could not find property '{}' in event state for entity {}";

    // Log messages (private - internal only)
    private static final String DEBUG_PROPERTY_UPDATED = "Updated property '{}' in Hibernate state for entity {}";

    /**
     * Update Hibernate event state with a new value for a specific property
     *
     * @param event the Hibernate PreInsertEvent
     * @param propertyName the name of the property to update
     * @param newValue the new value to set
     */
    public void updatePropertyInState(PreInsertEvent event, String propertyName, Object newValue) {
        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] state = event.getState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(propertyName)) {
                state[i] = newValue;
                log.debug(DEBUG_PROPERTY_UPDATED,
                        propertyName, event.getEntity().getClass().getSimpleName());
                return;
            }
        }

        log.warn(WARN_PROPERTY_NOT_FOUND,
                propertyName, event.getEntity().getClass().getSimpleName());
    }
}