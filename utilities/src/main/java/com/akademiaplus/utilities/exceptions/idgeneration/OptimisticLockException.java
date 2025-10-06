package com.akademiaplus.utilities.exceptions.idgeneration;

/**
 * Exception thrown when optimistic locking fails
 */
public class OptimisticLockException extends IDGenerationException {

    public OptimisticLockException(String entityName, Integer tenantId) {
        super(String.format("Optimistic lock failure for entity: %s, tenant: %d", entityName, tenantId));
    }
}
