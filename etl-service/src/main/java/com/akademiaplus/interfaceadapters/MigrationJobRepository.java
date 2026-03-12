/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link MigrationJob} documents.
 */
public interface MigrationJobRepository extends MongoRepository<MigrationJob, String> {

    /**
     * Finds jobs by tenant and status.
     *
     * @param tenantId the tenant identifier
     * @param status   the job status to filter by
     * @return list of matching jobs
     */
    List<MigrationJob> findByTenantIdAndStatus(Long tenantId, MigrationStatus status);

    /**
     * Finds all jobs for a tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of migration jobs
     */
    Page<MigrationJob> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Checks for duplicate file upload within a tenant.
     *
     * @param tenantId       the tenant identifier
     * @param sourceFileName the uploaded file name
     * @param status         the status to check (typically not FAILED)
     * @return the existing job if found
     */
    Optional<MigrationJob> findByTenantIdAndSourceFileNameAndStatusNot(
            Long tenantId, String sourceFileName, MigrationStatus status);
}
