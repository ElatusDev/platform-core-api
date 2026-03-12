/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate root for a data migration job.
 *
 * <p>Tracks the full lifecycle of an uploaded file from parsing through
 * Claude analysis, mapping confirmation, validation, and final load into MariaDB.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("migration_jobs")
public class MigrationJob {

    @Id
    private String id;

    private Long tenantId;

    private MigrationEntityType entityType;

    private String sourceFileName;

    private Long sourceFileSize;

    private MigrationStatus status;

    private int totalRows;

    private int validRows;

    private int errorRows;

    private int loadedRows;

    /** Claude API analysis results — one per sheet/table in the uploaded document. */
    private List<SheetAnalysis> documentAnalysis;

    private String analysisModel;

    private Instant analyzedAt;

    /** User-confirmed column mappings (may differ from Claude's suggestions). */
    private List<ColumnMapping> confirmedMappings;

    private String createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    private String errorMessage;
}
