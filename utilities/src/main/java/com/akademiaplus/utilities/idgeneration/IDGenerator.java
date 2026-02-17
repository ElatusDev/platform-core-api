package com.akademiaplus.utilities.idgeneration;

import com.akademiaplus.utilities.exceptions.idgeneration.IDGenerationException;

import java.util.List;

/**
 * Core interface for multi-tenant ID generation.
 * Implementations must ensure unique IDs within tenant/entity boundaries.
 */
public interface IDGenerator {

    /**
     * Generate a single unique ID for the specified entity and tenant.
     *
     * @param entityName The entity/table name (e.g., "course", "student")
     * @param tenantId The tenant identifier
     * @return A unique ID for this tenant/entity combination
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IDGenerationException if ID generation fails
     */
    Long generateId(String entityName, Long tenantId);

    /**
     * Generate multiple unique IDs in a single operation.
     * Useful for bulk imports and batch operations.
     *
     * @param entityName The entity/table name
     * @param tenantId The tenant identifier
     * @param count Number of IDs to generate
     * @return List of unique sequential IDs
     * @throws IllegalArgumentException if count <= 0 or parameters invalid
     * @throws IDGenerationException if generation fails
     */
    List<Long> generateIds(String entityName, Long tenantId, int count);

    /**
     * Reserve a range of IDs for external system integration.
     * Returns the starting ID of the reserved range.
     *
     * @param entityName The entity/table name
     * @param tenantId The tenant identifier
     * @param rangeSize Size of the range to reserve
     * @return The starting ID of the reserved range
     * @throws IllegalArgumentException if rangeSize <= 0
     * @throws IDGenerationException if reservation fails
     */
    Long reserveRange(String entityName, Long tenantId, int rangeSize);
}