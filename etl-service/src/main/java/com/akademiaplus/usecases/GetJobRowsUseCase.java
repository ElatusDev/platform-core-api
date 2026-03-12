/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Retrieves paginated migration rows with optional status filtering.
 */
@Service
public class GetJobRowsUseCase {

    private final MigrationRowRepository rowRepository;

    /**
     * Creates a new GetJobRowsUseCase.
     *
     * @param rowRepository the migration row repository
     */
    public GetJobRowsUseCase(MigrationRowRepository rowRepository) {
        this.rowRepository = rowRepository;
    }

    /**
     * Gets rows for a job with optional status filter and pagination.
     *
     * @param jobId    the migration job identifier
     * @param status   optional row status filter (null for all statuses)
     * @param pageable pagination parameters
     * @return page of migration rows
     */
    public Page<MigrationRow> getRows(String jobId, RowStatus status, Pageable pageable) {
        if (status != null) {
            return rowRepository.findByJobIdAndStatus(jobId, status, pageable);
        }
        return rowRepository.findByJobId(jobId, pageable);
    }
}
