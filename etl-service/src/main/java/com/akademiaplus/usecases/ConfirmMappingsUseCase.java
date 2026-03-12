/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.ColumnMapping;
import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.util.EntityFieldRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Accepts user-confirmed column mappings, validates required field coverage,
 * and triggers batch mapping application via {@link ApplyMappingUseCase}.
 */
@Service
public class ConfirmMappingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmMappingsUseCase.class);

    public static final String ERROR_JOB_NOT_FOUND = "Migration job not found: %s";
    public static final String ERROR_INVALID_STATUS = "Job must be in ANALYZED or VALIDATED status to confirm mappings, current status: %s";
    public static final String ERROR_MISSING_REQUIRED = "Confirmed mappings do not cover required fields: %s";
    public static final String ERROR_NO_ENTITY_TYPE = "Entity type must be set before confirming mappings";

    private final MigrationJobRepository jobRepository;
    private final ApplyMappingUseCase applyMappingUseCase;
    private final EntityFieldRegistry fieldRegistry;

    /**
     * Creates a new ConfirmMappingsUseCase.
     *
     * @param jobRepository      the migration job repository
     * @param applyMappingUseCase the batch mapping use case
     * @param fieldRegistry      the entity field registry
     */
    public ConfirmMappingsUseCase(MigrationJobRepository jobRepository,
                                   ApplyMappingUseCase applyMappingUseCase,
                                   EntityFieldRegistry fieldRegistry) {
        this.jobRepository = jobRepository;
        this.applyMappingUseCase = applyMappingUseCase;
        this.fieldRegistry = fieldRegistry;
    }

    /**
     * Confirms column mappings and triggers batch mapping application.
     *
     * @param jobId    the migration job identifier
     * @param mappings the user-confirmed column mappings
     * @return the updated migration job
     * @throws NoSuchElementException   if the job does not exist
     * @throws IllegalStateException    if the job is not in a valid status
     * @throws IllegalArgumentException if required fields are not covered
     */
    public MigrationJob execute(String jobId, List<ColumnMapping> mappings) {
        MigrationJob job = loadAndValidate(jobId);
        validateRequiredFieldsCovered(job, mappings);

        job.setConfirmedMappings(mappings);
        job.setStatus(MigrationStatus.MAPPING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        applyMappingUseCase.execute(jobId, mappings);

        log.info("Mappings confirmed — job={}, mappings={}", jobId, mappings.size());
        return jobRepository.findById(jobId).orElse(job);
    }

    private MigrationJob loadAndValidate(String jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(ERROR_JOB_NOT_FOUND, jobId)));

        if (job.getStatus() != MigrationStatus.ANALYZED
                && job.getStatus() != MigrationStatus.VALIDATED) {
            throw new IllegalStateException(
                    String.format(ERROR_INVALID_STATUS, job.getStatus()));
        }
        return job;
    }

    private void validateRequiredFieldsCovered(MigrationJob job, List<ColumnMapping> mappings) {
        if (job.getEntityType() == null) {
            throw new IllegalArgumentException(ERROR_NO_ENTITY_TYPE);
        }

        List<String> requiredFields = fieldRegistry.getRequiredFieldNames(job.getEntityType());
        Set<String> coveredTargets = new HashSet<>();
        for (ColumnMapping mapping : mappings) {
            coveredTargets.add(mapping.target());
            if (mapping.target().contains("+")) {
                for (String subField : mapping.target().split("\\+")) {
                    coveredTargets.add(subField.trim());
                }
            }
        }

        List<String> missing = requiredFields.stream()
                .filter(f -> !coveredTargets.contains(f))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format(ERROR_MISSING_REQUIRED, missing));
        }
    }
}
