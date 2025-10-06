package com.akademiaplus.utilities.idgeneration;

import com.akademiaplus.utilities.exceptions.idgeneration.IDGenerationException;
import com.akademiaplus.utilities.exceptions.idgeneration.OptimisticLockException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID Generator Service for Multi-Tenant Database with Composite Keys
 * Generates unique sequential IDs per tenant for each entity/table
 * Uses optimistic locking with version column for concurrent access
 *<p>
 * Compatible with Spring Boot 3.x and Jakarta EE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IDGeneratorService {

    private final JdbcTemplate jdbcTemplate;
    private final IDGeneratorProperties properties;

    // Cache for allocated ID ranges per tenant/entity
    // Structure: Map<tenantId, Map<entityName, IdRange>>
    private final Map<Integer, Map<String, IdRange>> allocatedRanges = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (properties.isAutoCreateTable()) {
            createSequenceTableIfNotExists();
        }
        log.info("IDGeneratorService initialized with batch size: {}, max retries: {}",
                properties.getDefaultBatchSize(), properties.getMaxRetryAttempts());
    }

    /**
     * Create sequence tracking table if it doesn't exist
     * This is for development/testing - production should manage schema separately
     */
    private void createSequenceTableIfNotExists() {
        try {
            // Check if table exists first
            jdbcTemplate.queryForObject(
                    "SELECT 1 FROM tenant_sequences LIMIT 1",
                    Integer.class
            );
            log.debug("Tenant sequences table already exists");
        } catch (Exception e) {
            log.info("Tenant sequences table not found, skipping auto-creation");
            // In production, table should be created by migration scripts
            // Auto-creation is disabled by default for safety
        }
    }

    /**
     * Generate next ID for a specific entity and tenant
     * Uses pre-allocated batches to minimize database calls
     *
     * @param entityName The entity/table name (e.g., "order", "customer", "product")
     * @param tenantId The tenant identifier
     * @return The next available ID for this tenant/entity combination
     */
    public Long generateId(String entityName, Integer tenantId) {
        return generateId(entityName, tenantId, properties.getDefaultBatchSize());
    }

    /**
     * Generate next ID with custom batch size
     * Useful for high-volume entities that need larger batches
     *
     * @param entityName The entity/table name
     * @param tenantId The tenant identifier
     * @param batchSize The number of IDs to pre-allocate
     * @return The next available ID
     */
    public Long generateId(String entityName, Integer tenantId, int batchSize) {
        validateParameters(entityName, tenantId, batchSize);

        log.trace("Generating ID for entity: {}, tenant: {}, batch size: {}",
                entityName, tenantId, batchSize);

        // Get or create the ranges map for this tenant
        Map<String, IdRange> entityRanges = allocatedRanges.computeIfAbsent(
                tenantId, k -> new ConcurrentHashMap<>()
        );

        // Try to get ID from existing range
        IdRange range = entityRanges.get(entityName);
        if (range != null && range.hasNext()) {
            long id = range.next();
            log.trace("Using pre-allocated ID {} for entity: {}, tenant: {} (remaining: {})",
                    id, entityName, tenantId, range.remaining());
            return id;
        }

        // Need to allocate a new batch from database
        synchronized (entityRanges) {
            // Double-check after acquiring lock
            range = entityRanges.get(entityName);
            if (range != null && range.hasNext()) {
                return range.next();
            }

            // Allocate new batch with retry on optimistic lock failure
            long nextBatchStart = allocateNextBatch(entityName, tenantId, batchSize);

            // Create and store new range
            IdRange newRange = new IdRange(
                    nextBatchStart,
                    nextBatchStart + batchSize - 1,
                    entityName,
                    tenantId
            );
            entityRanges.put(entityName, newRange);

            long id = newRange.next();
            log.debug("Allocated new batch [{} - {}] for entity: {}, tenant: {}",
                    nextBatchStart, nextBatchStart + batchSize - 1, entityName, tenantId);

            return id;
        }
    }

    /**
     * Validate input parameters
     */
    private void validateParameters(String entityName, Integer tenantId, int batchSize) {
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (batchSize > properties.getMaxBatchSize()) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum allowed %d",
                            batchSize, properties.getMaxBatchSize())
            );
        }
    }

    /**
     * Allocate next batch of IDs from database
     * Uses optimistic locking with version column
     * Retries automatically on concurrent modification
     */
    @Transactional
    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttemptsExpression = "#{@iDGeneratorProperties.maxRetryAttempts}",
            backoff = @Backoff(
                    delayExpression = "#{@iDGeneratorProperties.retryDelayMs}",
                    multiplierExpression = "#{@iDGeneratorProperties.retryMultiplier}"
            )
    )
    protected Long allocateNextBatch(String entityName, Integer tenantId, int batchSize) {
        log.debug("Allocating batch of {} IDs for entity: {}, tenant: {}",
                batchSize, entityName, tenantId);

        try {
            // Try to get existing sequence with version
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    "SELECT next_value, version FROM tenant_sequences WHERE tenant_id = ? AND entity_name = ?",
                    tenantId, entityName
            );

            Long currentNext = ((Number) result.get("next_value")).longValue();
            Integer version = ((Number) result.get("version")).intValue();

            // Update with optimistic locking check
            int updatedRows = jdbcTemplate.update(
                    "UPDATE tenant_sequences SET next_value = ?, version = version + 1 " +
                            "WHERE tenant_id = ? AND entity_name = ? AND version = ?",
                    currentNext + batchSize, tenantId, entityName, version
            );

            if (updatedRows == 0) {
                log.warn("Optimistic lock failure for entity: {}, tenant: {}. Retrying...",
                        entityName, tenantId);
                throw new OptimisticLockException(entityName, tenantId);
            }

            log.trace("Successfully allocated batch starting at {} for entity: {}, tenant: {}",
                    currentNext, entityName, tenantId);
            return currentNext;

        } catch (EmptyResultDataAccessException e) {
            log.debug("Sequence doesn't exist for entity: {}, tenant: {}, creating new one",
                    entityName, tenantId);
            return createNewSequence(entityName, tenantId, batchSize);
        } catch (OptimisticLockException e) {
            throw e; // Let retry handle this
        } catch (Exception e) {
            log.error("Error allocating batch for entity: {}, tenant: {}",
                    entityName, tenantId, e);
            throw new IDGenerationException(
                    String.format("Failed to allocate ID batch for entity: %s, tenant: %d",
                            entityName, tenantId), e);
        }
    }

    /**
     * Create a new sequence entry for a tenant/entity combination
     */
    private Long createNewSequence(String entityName, Integer tenantId, int batchSize) {
        try {
            // Try to insert new sequence
            jdbcTemplate.update(
                    "INSERT INTO tenant_sequences (tenant_id, entity_name, next_value, version) " +
                            "VALUES (?, ?, ?, 0)",
                    tenantId, entityName, batchSize + 1
            );

            log.info("Created new sequence for entity: {}, tenant: {} starting at 1",
                    entityName, tenantId);
            return 1L;

        } catch (Exception e) {
            // Another thread may have created it, retry allocation
            log.debug("Concurrent sequence creation detected, retrying allocation");
            return allocateNextBatch(entityName, tenantId, batchSize);
        }
    }

    /**
     * Get current next value without incrementing (for monitoring/debugging)
     */
    public Long peekNextValue(String entityName, Integer tenantId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT next_value FROM tenant_sequences WHERE tenant_id = ? AND entity_name = ?",
                    Long.class, tenantId, entityName
            );
        } catch (EmptyResultDataAccessException e) {
            log.debug("No sequence found for entity: {}, tenant: {}", entityName, tenantId);
            return 1L;
        }
    }

    /**
     * Reset a sequence to a specific value (use with caution!)
     * Only use this for data migration or fixing issues
     */
    @Transactional
    public void resetSequence(String entityName, Integer tenantId, Long newValue) {
        log.warn("DANGER: Resetting sequence for entity: {}, tenant: {} to value: {}",
                entityName, tenantId, newValue);

        jdbcTemplate.update(
                "INSERT INTO tenant_sequences (tenant_id, entity_name, next_value, version) " +
                        "VALUES (?, ?, ?, 0) " +
                        "ON DUPLICATE KEY UPDATE next_value = VALUES(next_value), version = version + 1",
                tenantId, entityName, newValue
        );

        // Clear cached range for this entity
        Map<String, IdRange> entityRanges = allocatedRanges.get(tenantId);
        if (entityRanges != null) {
            entityRanges.remove(entityName);
        }

        log.info("Reset sequence for entity: {}, tenant: {} to: {}",
                entityName, tenantId, newValue);
    }

    /**
     * Clear cached ranges for a specific tenant
     * Useful after bulk operations or migrations
     */
    public void clearCache(Integer tenantId) {
        Map<String, IdRange> removed = allocatedRanges.remove(tenantId);
        if (removed != null) {
            log.info("Cleared {} cached ranges for tenant: {}", removed.size(), tenantId);
        }
    }

    /**
     * Clear cached ranges for a specific entity across all tenants
     */
    public void clearEntityCache(String entityName) {
        int cleared = 0;
        for (Map<String, IdRange> entityRanges : allocatedRanges.values()) {
            if (entityRanges.remove(entityName) != null) {
                cleared++;
            }
        }
        log.info("Cleared cached ranges for entity: {} across {} tenants", entityName, cleared);
    }

    /**
     * Clear all cached ranges
     */
    public void clearAllCaches() {
        int tenantCount = allocatedRanges.size();
        allocatedRanges.clear();
        log.info("Cleared all ID caches for {} tenants", tenantCount);
    }

    /**
     * Get statistics about cached ranges (for monitoring)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalTenants", allocatedRanges.size());

        int totalRanges = 0;
        long totalCachedIds = 0;

        for (Map<String, IdRange> entityRanges : allocatedRanges.values()) {
            totalRanges += entityRanges.size();
            for (IdRange range : entityRanges.values()) {
                totalCachedIds += range.remaining();
            }
        }

        stats.put("totalCachedRanges", totalRanges);
        stats.put("totalCachedIds", totalCachedIds);
        stats.put("defaultBatchSize", properties.getDefaultBatchSize());

        // Detailed stats per tenant (optional, might be verbose)
        if (allocatedRanges.size() <= 10) { // Only include details for small number of tenants
            Map<Integer, Map<String, String>> tenantStats = new ConcurrentHashMap<>();
            allocatedRanges.forEach((tenantId, entities) -> {
                Map<String, String> entityStats = new ConcurrentHashMap<>();
                entities.forEach((entity, range) -> {
                    entityStats.put(entity, String.format("remaining=%d", range.remaining()));
                });
                tenantStats.put(tenantId, entityStats);
            });
            stats.put("rangeDetails", tenantStats);
        }

        return stats;
    }
}
