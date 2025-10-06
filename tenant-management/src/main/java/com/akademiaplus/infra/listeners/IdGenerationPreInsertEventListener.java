package com.akademiaplus.infra.listeners;

import com.akademiaplus.infra.EntityIdAssigner;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.stereotype.Component;

@Component
public class IdGenerationPreInsertEventListener implements PreInsertEventListener {

    private final EntityIdAssigner entityIdAssigner;

    public IdGenerationPreInsertEventListener(EntityIdAssigner entityIdAssigner) {
        this.entityIdAssigner = entityIdAssigner;
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        try {
            entityIdAssigner.assignIdIfNeeded(event.getEntity(), event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ID for entity: " +
                    event.getEntity().getClass().getName(), e);
        }

        return false;
    }
}