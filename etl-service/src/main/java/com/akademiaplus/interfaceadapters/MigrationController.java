/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.domain.ColumnMapping;
import com.akademiaplus.domain.MigrationEntityType;
import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.RollbackResult;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.usecases.AnalyzeStructureUseCase;
import com.akademiaplus.usecases.ConfirmMappingsUseCase;
import com.akademiaplus.usecases.GetJobRowsUseCase;
import com.akademiaplus.usecases.GetJobStatusUseCase;
import com.akademiaplus.usecases.LoadIntoMariaDbUseCase;
import com.akademiaplus.usecases.RollbackJobUseCase;
import com.akademiaplus.usecases.UploadDocumentUseCase;
import com.akademiaplus.usecases.ValidateJobUseCase;
import com.akademiaplus.util.EntityFieldDefinition;
import com.akademiaplus.util.EntityFieldRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for ETL migration operations.
 *
 * <p>Provides endpoints for the full ETL pipeline: upload, analyze,
 * map, validate, load into MariaDB, and rollback.</p>
 */
@RestController
@RequestMapping("/v1/etl/migrations")
public class MigrationController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final AnalyzeStructureUseCase analyzeStructureUseCase;
    private final ConfirmMappingsUseCase confirmMappingsUseCase;
    private final ValidateJobUseCase validateJobUseCase;
    private final LoadIntoMariaDbUseCase loadIntoMariaDbUseCase;
    private final RollbackJobUseCase rollbackJobUseCase;
    private final GetJobStatusUseCase getJobStatusUseCase;
    private final GetJobRowsUseCase getJobRowsUseCase;
    private final EntityFieldRegistry entityFieldRegistry;

    /**
     * Creates a new MigrationController.
     *
     * @param uploadDocumentUseCase   the upload use case
     * @param analyzeStructureUseCase the analyze use case
     * @param confirmMappingsUseCase  the confirm mappings use case
     * @param validateJobUseCase      the validate use case
     * @param loadIntoMariaDbUseCase  the load use case
     * @param rollbackJobUseCase      the rollback use case
     * @param getJobStatusUseCase     the job status use case
     * @param getJobRowsUseCase       the job rows use case
     * @param entityFieldRegistry     the entity field registry
     */
    public MigrationController(UploadDocumentUseCase uploadDocumentUseCase,
                                AnalyzeStructureUseCase analyzeStructureUseCase,
                                ConfirmMappingsUseCase confirmMappingsUseCase,
                                ValidateJobUseCase validateJobUseCase,
                                LoadIntoMariaDbUseCase loadIntoMariaDbUseCase,
                                RollbackJobUseCase rollbackJobUseCase,
                                GetJobStatusUseCase getJobStatusUseCase,
                                GetJobRowsUseCase getJobRowsUseCase,
                                EntityFieldRegistry entityFieldRegistry) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.analyzeStructureUseCase = analyzeStructureUseCase;
        this.confirmMappingsUseCase = confirmMappingsUseCase;
        this.validateJobUseCase = validateJobUseCase;
        this.loadIntoMariaDbUseCase = loadIntoMariaDbUseCase;
        this.rollbackJobUseCase = rollbackJobUseCase;
        this.getJobStatusUseCase = getJobStatusUseCase;
        this.getJobRowsUseCase = getJobRowsUseCase;
        this.entityFieldRegistry = entityFieldRegistry;
    }

    /**
     * Uploads an Excel or Word file to start a new migration job.
     *
     * @param file       the file to upload (multipart)
     * @param entityType optional entity type hint
     * @param tenantId   the tenant identifier (from header)
     * @return 201 Created with the created migration job
     */
    @PostMapping("/upload")
    public ResponseEntity<MigrationJob> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityType", required = false) MigrationEntityType entityType,
            @RequestParam("tenantId") Long tenantId) {
        MigrationJob job = uploadDocumentUseCase.execute(file, entityType, tenantId, "system");
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    /**
     * Lists all migration jobs for a tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return 200 OK with page of migration jobs
     */
    @GetMapping
    public ResponseEntity<Page<MigrationJob>> listJobs(
            @RequestParam("tenantId") Long tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(getJobStatusUseCase.listJobs(tenantId, pageable));
    }

    /**
     * Gets details for a specific migration job.
     *
     * @param jobId the job identifier
     * @return 200 OK with the migration job
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<MigrationJob> getJob(@PathVariable String jobId) {
        return ResponseEntity.ok(getJobStatusUseCase.getJob(jobId));
    }

    /**
     * Triggers Claude API analysis for a parsed migration job.
     *
     * @param jobId the job identifier
     * @return 200 OK with the updated migration job containing analysis results
     */
    @PostMapping("/{jobId}/analyze")
    public ResponseEntity<MigrationJob> analyzeJob(@PathVariable String jobId) {
        return ResponseEntity.ok(analyzeStructureUseCase.execute(jobId));
    }

    /**
     * Lists all supported entity types with their field definitions.
     *
     * @return 200 OK with entity types and fields
     */
    @GetMapping("/entity-types")
    public ResponseEntity<Map<MigrationEntityType, List<EntityFieldDefinition>>> getEntityTypes() {
        return ResponseEntity.ok(entityFieldRegistry.getAllFields());
    }

    /**
     * Confirms column mappings and triggers batch mapping application.
     *
     * @param jobId    the job identifier
     * @param mappings the user-confirmed column mappings
     * @return 200 OK with the updated migration job
     */
    @PutMapping("/{jobId}/mappings")
    public ResponseEntity<MigrationJob> confirmMappings(
            @PathVariable String jobId,
            @RequestBody List<ColumnMapping> mappings) {
        return ResponseEntity.ok(confirmMappingsUseCase.execute(jobId, mappings));
    }

    /**
     * Validates all mapped rows against the entity schema.
     *
     * @param jobId the job identifier
     * @return 200 OK with the updated migration job containing validation results
     */
    @PostMapping("/{jobId}/validate")
    public ResponseEntity<MigrationJob> validateJob(@PathVariable String jobId) {
        return ResponseEntity.ok(validateJobUseCase.execute(jobId));
    }

    /**
     * Lists rows for a migration job with optional status filter and pagination.
     *
     * @param jobId    the job identifier
     * @param status   optional row status filter
     * @param pageable pagination parameters
     * @return 200 OK with page of migration rows
     */
    @GetMapping("/{jobId}/rows")
    public ResponseEntity<Page<MigrationRow>> getJobRows(
            @PathVariable String jobId,
            @RequestParam(value = "status", required = false) RowStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(getJobRowsUseCase.getRows(jobId, status, pageable));
    }

    /**
     * Loads validated rows into MariaDB.
     *
     * @param jobId the job identifier
     * @return 202 Accepted with the updated migration job
     */
    @PostMapping("/{jobId}/load")
    public ResponseEntity<MigrationJob> loadJob(@PathVariable String jobId) {
        return ResponseEntity.accepted().body(loadIntoMariaDbUseCase.execute(jobId));
    }

    /**
     * Rolls back loaded entities by soft-deleting them from MariaDB
     * and resetting row statuses to VALID.
     *
     * @param jobId the job identifier
     * @return 200 OK with the rollback result
     */
    @DeleteMapping("/{jobId}/rollback")
    public ResponseEntity<RollbackResult> rollbackJob(@PathVariable String jobId) {
        return ResponseEntity.ok(rollbackJobUseCase.execute(jobId));
    }
}
