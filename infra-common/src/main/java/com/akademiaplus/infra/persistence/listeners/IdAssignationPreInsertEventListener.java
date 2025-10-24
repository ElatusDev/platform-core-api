package com.akademiaplus.infra.persistence.listeners;

import com.akademiaplus.infra.persistence.exceptions.IdAssignmentException;
import com.akademiaplus.infra.persistence.idassigner.EntityIdAssigner;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.stereotype.Component;

/**
 * Hibernate event listener that assigns IDs to entities before insert
 * Integrates with EntityIdAssigner to ensure all entities have proper IDs
 */
@Slf4j
@Component
public class IdAssignationPreInsertEventListener implements PreInsertEventListener {

    // Error messages (public for testing)
    public static final String ERROR_FAILED_TO_GENERATE_ID = "Failed to generate ID for entity: {}";

    private final EntityIdAssigner entityIdAssigner;

    public IdAssignationPreInsertEventListener(EntityIdAssigner entityIdAssigner) {
        this.entityIdAssigner = entityIdAssigner;
    }

    /**
     * Called before an entity is inserted into the database
     *
     * @param event the pre-insert event containing entity information
     * @return false to indicate normal processing should continue
     * @throws IdAssignmentException if ID assignment fails
     */
    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        entityIdAssigner.assignIdIfNeeded(event.getEntity(), event);
        return false;
    }
}