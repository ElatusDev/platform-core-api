/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.RowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link MigrationRow} documents.
 */
public interface MigrationRowRepository extends MongoRepository<MigrationRow, String> {

    /**
     * Finds rows for a job with pagination.
     *
     * @param jobId    the migration job identifier
     * @param pageable pagination parameters
     * @return page of migration rows
     */
    Page<MigrationRow> findByJobId(String jobId, Pageable pageable);

    /**
     * Finds all rows for a job with a specific status.
     *
     * @param jobId  the migration job identifier
     * @param status the row status to filter by
     * @return list of matching rows
     */
    List<MigrationRow> findByJobIdAndStatus(String jobId, RowStatus status);

    /**
     * Counts rows for a job with a specific status.
     *
     * @param jobId  the migration job identifier
     * @param status the row status to count
     * @return number of rows matching the criteria
     */
    long countByJobIdAndStatus(String jobId, RowStatus status);

    /**
     * Finds rows for a job filtered by status with pagination.
     *
     * @param jobId    the migration job identifier
     * @param status   the row status to filter by
     * @param pageable pagination parameters
     * @return page of matching rows
     */
    Page<MigrationRow> findByJobIdAndStatus(String jobId, RowStatus status, Pageable pageable);

    /**
     * Deletes all rows belonging to a job.
     *
     * @param jobId the migration job identifier
     */
    void deleteByJobId(String jobId);
}
