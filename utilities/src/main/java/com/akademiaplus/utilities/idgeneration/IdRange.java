package com.akademiaplus.utilities.idgeneration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a range of pre-allocated IDs
 * Thread-safe implementation for concurrent access
 */
@Slf4j
@Getter
public class IdRange {
    private long current;
    private final long max;
    private final String entityName;
    private final Integer tenantId;

    public IdRange(long start, long max, String entityName, Integer tenantId) {
        this.current = start;
        this.max = max;
        this.entityName = entityName;
        this.tenantId = tenantId;
    }

    public synchronized boolean hasNext() {
        return current <= max;
    }

    public synchronized long next() {
        if (!hasNext()) {
            log.warn("ID range exhausted for entity: {}, tenant: {}", entityName, tenantId);
            throw new IllegalStateException("No more IDs available in range");
        }
        long id = current++;
        log.trace("Allocated ID {} from range for entity: {}, tenant: {}", id, entityName, tenantId);
        return id;
    }

    public synchronized long remaining() {
        return hasNext() ? (max - current + 1) : 0;
    }

    @Override
    public String toString() {
        return String.format("IdRange[entity=%s, tenant=%d, current=%d, max=%d, remaining=%d]",
                entityName, tenantId, current, max, remaining());
    }
}