package com.akademiaplus.infra.persistence.idassigner;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Handles Hibernate-specific operations for updating event state.
 *
 * <p>For entities with {@code @IdClass} composite keys, the {@code @Id} fields
 * are <strong>not</strong> included in {@link org.hibernate.persister.entity.EntityPersister#getPropertyNames()}.
 * They live in the identifier object returned by {@code event.getId()}.
 * This class tries the property state array first, then falls back to reflective
 * identifier update so that IDs assigned during {@code PreInsert} are visible
 * in the SQL INSERT — especially for cascaded child entities.</p>
 */
@Slf4j
@Component
public class HibernateStateUpdater {

    // Log messages (public for testing)
    public static final String WARN_PROPERTY_NOT_FOUND =
            "Could not find property '{}' in event state or identifier for entity {}";

    // Log messages (private - internal only)
    private static final String DEBUG_PROPERTY_UPDATED =
            "Updated property '{}' in Hibernate state for entity {}";
    private static final String DEBUG_IDENTIFIER_UPDATED =
            "Updated property '{}' in identifier object for entity {}";

    /**
     * Update Hibernate event state with a new value for a specific property.
     *
     * <p>Tries the regular property state array first. If the property is not
     * found (typical for {@code @Id} fields in {@code @IdClass} composites),
     * falls back to reflective update of the identifier object.</p>
     *
     * @param event        the Hibernate PreInsertEvent
     * @param propertyName the name of the property to update
     * @param newValue     the new value to set
     */
    public void updatePropertyInState(PreInsertEvent event, String propertyName, Object newValue) {
        if (tryUpdatePropertyState(event, propertyName, newValue)) {
            return;
        }

        if (tryUpdateIdentifier(event, propertyName, newValue)) {
            return;
        }

        log.warn(WARN_PROPERTY_NOT_FOUND,
                propertyName, event.getEntity().getClass().getSimpleName());
    }

    /**
     * Attempts to update the value in the regular property state array.
     *
     * @return {@code true} if the property was found and updated
     */
    private boolean tryUpdatePropertyState(PreInsertEvent event, String propertyName, Object newValue) {
        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] state = event.getState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(propertyName)) {
                state[i] = newValue;
                log.debug(DEBUG_PROPERTY_UPDATED,
                        propertyName, event.getEntity().getClass().getSimpleName());
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to update the value in the event's identifier object.
     *
     * <p>For {@code @IdClass} composite keys, the identifier is an instance of
     * the IdClass with fields matching the entity's {@code @Id} fields.
     * Reflectively sets the matching field on the identifier.</p>
     *
     * @return {@code true} if the identifier field was found and updated
     */
    private boolean tryUpdateIdentifier(PreInsertEvent event, String propertyName, Object newValue) {
        Object id = event.getId();
        if (id == null) {
            return false;
        }

        try {
            Field field = id.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            field.set(id, newValue);
            log.debug(DEBUG_IDENTIFIER_UPDATED,
                    propertyName, event.getEntity().getClass().getSimpleName());
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
}