package com.akademiaplus.utilities.idgeneration;

import com.akademiaplus.utilities.exceptions.idgeneration.IDGenerationException;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequenceRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.LongStream;

/**
 * Sequential ID generator implementation for multi-tenant systems.
 * <p>
 * Generates unique, sequential IDs per tenant/entity combination.
 * Uses optimistic locking with automatic retry for concurrent access.
 * <p>
 * Features:
 * <ul>
 *   <li>Per-tenant sequence isolation</li>
 *   <li>Optimistic locking with automatic retry (up to 3 attempts)</li>
 *   <li>Batch ID generation support</li>
 *   <li>ID range reservation for pre-allocation</li>
 *   <li>Overflow protection</li>
 * </ul>
 */
@Slf4j
@Service
public class SequentialIDGenerator implements IDGenerator {

    // Sequence configuration
    private static final long INITIAL_SEQUENCE_VALUE = 1L;
    private static final int INITIAL_VERSION = 0;

    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_DELAY = 50L;
    private static final int RETRY_BACKOFF_MULTIPLIER = 2;

    // Validation limits
    private static final int MAX_BATCH_SIZE = 10000;
    private static final int MIN_TENANT_ID = 0;

    // Validation error messages (public for testing)
    public static final String ERROR_ENTITY_NAME_EMPTY = "Entity name cannot be null or empty";
    public static final String ERROR_TENANT_ID_NULL = "Tenant ID cannot be null";
    public static final String ERROR_TENANT_ID_NOT_POSITIVE = "Tenant ID must be positive";
    public static final String ERROR_COUNT_NOT_POSITIVE = "Count must be positive";
    public static final String ERROR_COUNT_EXCEEDS_LIMIT = "Count cannot exceed %d for safety";

    // Business error messages (public for testing)
    public static final String ERROR_ID_RANGE_OVERFLOW_ALLOCATE =
            "ID range overflow for entity: %s, tenant: %d. Cannot allocate %d IDs starting from %d";
    public static final String ERROR_ID_RANGE_OVERFLOW_RESERVE =
            "ID range overflow for entity: %s, tenant: %d. Cannot reserve %d IDs starting from %d";
    public static final String ERROR_FAILED_TO_INITIALIZE_SEQUENCE =
            "Failed to initialize sequence for entity: %s, tenant: %d";

    // Log messages (private - internal only)
    private static final String DEBUG_GENERATING_ID = "Generating ID for entity: {}, tenant: {}";
    private static final String TRACE_GENERATED_ID = "Generated ID {} for entity: {}, tenant: {}";
    private static final String DEBUG_OPTIMISTIC_LOCK_RETRY =
            "Optimistic lock failure for entity: {}, tenant: {}. Retrying...";
    private static final String DEBUG_GENERATING_IDS_BATCH = "Generating {} IDs for entity: {}, tenant: {}";
    private static final String TRACE_GENERATED_IDS_BATCH =
            "Generated {} IDs [{} - {}] for entity: {}, tenant: {}";
    private static final String DEBUG_OPTIMISTIC_LOCK_BATCH_RETRY =
            "Optimistic lock failure during batch generation for entity: {}, tenant: {}. Retrying...";
    private static final String DEBUG_RESERVING_RANGE = "Reserving range of {} IDs for entity: {}, tenant: {}";
    private static final String INFO_RESERVED_RANGE = "Reserved ID range [{} - {}] for entity: {}, tenant: {}";
    private static final String DEBUG_OPTIMISTIC_LOCK_RANGE_RETRY =
            "Optimistic lock failure during range reservation for entity: {}, tenant: {}. Retrying...";
    private static final String INFO_INITIALIZING_SEQUENCE = "Initializing new sequence for entity: {}, tenant: {}";
    private static final String DEBUG_CONCURRENT_CREATION =
            "Concurrent sequence creation detected for entity: {}, tenant: {}";

    private final TenantSequenceRepository repository;

    /**
     * Constructor for dependency injection.
     *
     * @param repository The repository for managing tenant sequences
     */
    public SequentialIDGenerator(TenantSequenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Generates a single sequential ID for the specified entity and tenant.
     * <p>
     * This method uses optimistic locking and will automatically retry up to 3 times
     * in case of concurrent access conflicts.
     *
     * @param entityName The name of the entity (e.g., table name)
     * @param tenantId The tenant identifier
     * @return The generated ID
     * @throws IDGenerationException if ID generation fails
     * @throws IllegalArgumentException if parameters are invalid
     */
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            retryFor = {OptimisticLockException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = RETRY_BACKOFF_DELAY, multiplier = RETRY_BACKOFF_MULTIPLIER)
    )
    public Long generateId(String entityName, Long tenantId) {
        validateParameters(entityName, tenantId);

        log.debug(DEBUG_GENERATING_ID, entityName, tenantId);

        TenantSequence.TenantSequenceId sequenceId =
                new TenantSequence.TenantSequenceId(tenantId, entityName);

        TenantSequence sequence = repository.findById(sequenceId)
                .orElseGet(() -> initializeSequence(tenantId, entityName));

        Long nextId = sequence.getNextValue();
        sequence.setNextValue(nextId + 1);

        try {
            repository.save(sequence);
            log.trace(TRACE_GENERATED_ID, nextId, entityName, tenantId);
            return nextId;

        } catch (OptimisticLockException e) {
            log.debug(DEBUG_OPTIMISTIC_LOCK_RETRY, entityName, tenantId);
            throw e;
        }
    }

    /**
     * Generates a batch of sequential IDs for the specified entity and tenant.
     * <p>
     * This method is more efficient than calling generateId() multiple times
     * as it reserves a range of IDs in a single transaction.
     *
     * @param entityName The name of the entity (e.g., table name)
     * @param tenantId The tenant identifier
     * @param count The number of IDs to generate (max 10000)
     * @return List of generated IDs
     * @throws IDGenerationException if ID generation fails or overflow occurs
     * @throws IllegalArgumentException if parameters are invalid
     */
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            retryFor = {OptimisticLockException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = RETRY_BACKOFF_DELAY, multiplier = RETRY_BACKOFF_MULTIPLIER)
    )
    public List<Long> generateIds(String entityName, Long tenantId, int count) {
        validateParameters(entityName, tenantId);
        validateCount(count);

        log.debug(DEBUG_GENERATING_IDS_BATCH, count, entityName, tenantId);

        TenantSequence.TenantSequenceId sequenceId =
                new TenantSequence.TenantSequenceId(tenantId, entityName);

        TenantSequence sequence = repository.findById(sequenceId)
                .orElseGet(() -> initializeSequence(tenantId, entityName));

        Long startId = sequence.getNextValue();
        long endId = startId + count;

        // Check for overflow
        if (endId < startId) {
            throw new IDGenerationException(
                    String.format(ERROR_ID_RANGE_OVERFLOW_ALLOCATE, entityName, tenantId, count, startId));
        }

        sequence.setNextValue(endId);

        try {
            repository.save(sequence);

            List<Long> ids = LongStream.range(startId, endId)
                    .boxed()
                    .toList();

            log.trace(TRACE_GENERATED_IDS_BATCH, count, startId, endId - 1, entityName, tenantId);

            return ids;

        } catch (OptimisticLockException e) {
            log.debug(DEBUG_OPTIMISTIC_LOCK_BATCH_RETRY, entityName, tenantId);
            throw e;
        }
    }

    /**
     * Reserves a range of IDs for future use without immediately allocating them.
     * <p>
     * This is useful for pre-allocating ID ranges for batch operations or caching.
     * Returns the starting ID of the reserved range.
     *
     * @param entityName The name of the entity (e.g., table name)
     * @param tenantId The tenant identifier
     * @param rangeSize The size of the range to reserve (max 10000)
     * @return The starting ID of the reserved range
     * @throws IDGenerationException if reservation fails or overflow occurs
     * @throws IllegalArgumentException if parameters are invalid
     */
    @Override
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            retryFor = {OptimisticLockException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = RETRY_BACKOFF_DELAY, multiplier = RETRY_BACKOFF_MULTIPLIER)
    )
    public Long reserveRange(String entityName, Long tenantId, int rangeSize) {
        validateParameters(entityName, tenantId);
        validateCount(rangeSize);

        log.debug(DEBUG_RESERVING_RANGE, rangeSize, entityName, tenantId);

        TenantSequence.TenantSequenceId sequenceId =
                new TenantSequence.TenantSequenceId(tenantId, entityName);

        TenantSequence sequence = repository.findById(sequenceId)
                .orElseGet(() -> initializeSequence(tenantId, entityName));

        Long startId = sequence.getNextValue();
        long endId = startId + rangeSize;

        // Check for overflow
        if (endId < startId) {
            throw new IDGenerationException(
                    String.format(ERROR_ID_RANGE_OVERFLOW_RESERVE, entityName, tenantId, rangeSize, startId));
        }

        sequence.setNextValue(endId);

        try {
            repository.save(sequence);

            log.info(INFO_RESERVED_RANGE, startId, endId - 1, entityName, tenantId);

            return startId;

        } catch (OptimisticLockException e) {
            log.debug(DEBUG_OPTIMISTIC_LOCK_RANGE_RETRY, entityName, tenantId);
            throw e;
        }
    }

    /**
     * Initialize a new sequence for a tenant/entity combination.
     * Handles concurrent creation attempts gracefully using DataIntegrityViolationException.
     *
     * @param tenantId The tenant identifier
     * @param entityName The entity name
     * @return The initialized sequence
     * @throws IDGenerationException if initialization fails after concurrent creation
     */
    private TenantSequence initializeSequence(Long tenantId, String entityName) {
        log.info(INFO_INITIALIZING_SEQUENCE, entityName, tenantId);

        TenantSequence newSequence = TenantSequence.builder()
                .id(new TenantSequence.TenantSequenceId(tenantId, entityName))
                .nextValue(INITIAL_SEQUENCE_VALUE)
                .version(INITIAL_VERSION)
                .build();

        try {
            return repository.saveAndFlush(newSequence);

        } catch (DataIntegrityViolationException e) {
            // Another thread created it concurrently
            log.debug(DEBUG_CONCURRENT_CREATION, entityName, tenantId);

            return repository.findById(newSequence.getId())
                    .orElseThrow(() -> new IDGenerationException(
                            String.format(ERROR_FAILED_TO_INITIALIZE_SEQUENCE, entityName, tenantId), e));
        }
    }

    /**
     * Validate entity name and tenant ID parameters.
     *
     * @param entityName The entity name to validate
     * @param tenantId The tenant ID to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateParameters(String entityName, Long tenantId) {
        Assert.hasText(entityName, ERROR_ENTITY_NAME_EMPTY);
        Assert.notNull(tenantId, ERROR_TENANT_ID_NULL);
        Assert.isTrue(tenantId > MIN_TENANT_ID, ERROR_TENANT_ID_NOT_POSITIVE);
    }

    /**
     * Validate count/range size parameter.
     *
     * @param count The count to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCount(int count) {
        Assert.isTrue(count > MIN_TENANT_ID, ERROR_COUNT_NOT_POSITIVE);
        Assert.isTrue(count <= MAX_BATCH_SIZE,
                String.format(ERROR_COUNT_EXCEEDS_LIMIT, MAX_BATCH_SIZE));
    }
}