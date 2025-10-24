package com.akademiaplus.infra.persistence.idassigner;

import org.springframework.stereotype.Component;

/**
 * Determines when an ID should be generated for an entity
 * Encapsulates the business logic for ID generation decisions
 */
@Component
public class IdGenerationStrategy {

    /**
     * Check if ID should be generated based on current value
     *
     * @param currentId the current ID value of the entity
     * @return true if a new ID should be generated, false otherwise
     */
    public boolean shouldGenerateId(Object currentId) {
        if (currentId == null) {
            return true;
        }

        // Handle empty strings
        if (currentId instanceof String && ((String) currentId).isEmpty()) {
            return true;
        }

        // Handle zero for numeric types (optional - depends on your business logic)
        return currentId instanceof Number && ((Number) currentId).longValue() == 0L;
    }
}