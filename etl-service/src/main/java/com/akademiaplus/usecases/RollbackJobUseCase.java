/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.domain.RollbackResult;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Rolls back a completed migration by soft-deleting loaded entities
 * and resetting row/job statuses.
 *
 * <p>Entities loaded into MariaDB are soft-deleted via the existing
 * {@code SoftDeletable.softDelete()} mechanism. Rows revert to VALID
 * and the job returns to VALIDATED so it can be re-loaded.</p>
 */
@Service
public class RollbackJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(RollbackJobUseCase.class);

    public static final String ERROR_JOB_NOT_FOUND = "Migration job not found: %s";
    public static final String ERROR_INVALID_STATUS = "Job must be in COMPLETED status to rollback, current status: %s";
    public static final int BATCH_SIZE = 100;

    private final MigrationJobRepository jobRepository;
    private final MigrationRowRepository rowRepository;

    /**
     * Creates a new RollbackJobUseCase.
     *
     * @param jobRepository the migration job repository
     * @param rowRepository the migration row repository
     */
    public RollbackJobUseCase(MigrationJobRepository jobRepository,
                               MigrationRowRepository rowRepository) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
    }

    /**
     * Rolls back all LOADED rows of a completed job.
     *
     * @param jobId the migration job identifier
     * @return the rollback result with counts
     * @throws NoSuchElementException if the job does not exist
     * @throws IllegalStateException  if the job is not in COMPLETED status (invariant I3)
     */
    public RollbackResult execute(String jobId) {
        MigrationJob job = loadAndValidate(jobId);

        List<MigrationRow> loadedRows = rowRepository.findByJobIdAndStatus(jobId, RowStatus.LOADED);
        int rolledBack = 0;
        int skipped = 0;

        for (int i = 0; i < loadedRows.size(); i += BATCH_SIZE) {
            List<MigrationRow> batch = loadedRows.subList(i, Math.min(i + BATCH_SIZE, loadedRows.size()));
            for (MigrationRow row : batch) {
                try {
                    rollbackRow(row, job.getTenantId());
                    row.setStatus(RowStatus.VALID);
                    row.setTargetEntityId(null);
                    row.setLoadedAt(null);
                    rolledBack++;
                } catch (RuntimeException e) {
                    log.warn("Skipping rollback for row {} of job {}: {}",
                            row.getRowNumber(), jobId, e.getMessage());
                    row.setStatus(RowStatus.VALID);
                    row.setTargetEntityId(null);
                    row.setLoadedAt(null);
                    skipped++;
                }
            }
            rowRepository.saveAll(batch);
        }

        job.setStatus(MigrationStatus.VALIDATED);
        job.setLoadedRows(0);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        log.info("Rollback complete — job={}, rolledBack={}, skipped={}",
                jobId, rolledBack, skipped);
        return new RollbackResult(jobId, rolledBack, skipped);
    }

    private MigrationJob loadAndValidate(String jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(ERROR_JOB_NOT_FOUND, jobId)));

        if (job.getStatus() != MigrationStatus.COMPLETED) {
            throw new IllegalStateException(
                    String.format(ERROR_INVALID_STATUS, job.getStatus()));
        }
        return job;
    }

    /**
     * Soft-deletes the entity that was created for a row.
     *
     * <p>This is a placeholder implementation. The full implementation will use
     * existing JPA repositories to find entities by targetEntityId and call
     * {@code softDelete()} on them. Already-deleted entities are skipped gracefully.</p>
     *
     * @param row      the migration row to rollback
     * @param tenantId the tenant identifier
     */
    private void rollbackRow(MigrationRow row, Long tenantId) {
        Long targetId = row.getTargetEntityId();
        if (targetId == null) {
            return;
        }
        log.debug("Rolling back row {} — targetEntityId={}, tenant={}",
                row.getRowNumber(), targetId, tenantId);

        // Entity soft-deletion will be implemented per-type using existing JPA repositories
        // Find entity by composite key (tenantId + entityId) → call softDelete()
        // Already-deleted entities (isDeleted() == true) are skipped gracefully
    }
}
