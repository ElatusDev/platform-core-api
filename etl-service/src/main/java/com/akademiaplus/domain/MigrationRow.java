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
import java.util.Map;

/**
 * Represents a single row from an uploaded document, tracked through the ETL pipeline.
 *
 * <p>Each row transitions through: RAW → MAPPED → VALID/INVALID → LOADED/LOAD_FAILED.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("migration_rows")
public class MigrationRow {

    @Id
    private String id;

    private String jobId;

    private String sheetName;

    private int rowNumber;

    private RowStatus status;

    /** Raw cell values keyed by column header from the source document. */
    private Map<String, String> rawData;

    /** Mapped values keyed by target field name after transformation. */
    private Map<String, String> mappedData;

    private List<ValidationError> validationErrors;

    /** ID of the entity created in MariaDB after successful load. */
    private Long targetEntityId;

    private Instant loadedAt;
}
