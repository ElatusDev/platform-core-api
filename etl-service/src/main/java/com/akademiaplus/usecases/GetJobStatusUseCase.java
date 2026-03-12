/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Retrieves migration job details and listings.
 */
@Service
public class GetJobStatusUseCase {

    public static final String ERROR_JOB_NOT_FOUND = "Migration job not found: %s";

    private final MigrationJobRepository jobRepository;

    /**
     * Creates a new GetJobStatusUseCase.
     *
     * @param jobRepository the migration job repository
     */
    public GetJobStatusUseCase(MigrationJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Gets a migration job by ID.
     *
     * @param jobId the job identifier
     * @return the migration job
     * @throws NoSuchElementException if the job does not exist
     */
    public MigrationJob getJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(ERROR_JOB_NOT_FOUND, jobId)));
    }

    /**
     * Lists all migration jobs for a tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of migration jobs
     */
    public Page<MigrationJob> listJobs(Long tenantId, Pageable pageable) {
        return jobRepository.findByTenantId(tenantId, pageable);
    }
}
