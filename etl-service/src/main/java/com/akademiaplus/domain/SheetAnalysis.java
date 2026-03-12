/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

import java.util.List;

/**
 * Claude API analysis result for a single sheet or table within an uploaded document.
 *
 * @param sheetName             the sheet or table name from the source document
 * @param detectedEntityType    the entity type detected by Claude (may be null if uncertain)
 * @param confidence            confidence score from 0.0 to 1.0
 * @param columnMappings        suggested column-to-field mappings
 * @param warnings              non-blocking issues detected during analysis
 * @param unmappedSourceColumns source columns that could not be mapped to any target field
 * @param missingRequiredFields required target fields with no matching source column
 */
public record SheetAnalysis(
        String sheetName,
        String detectedEntityType,
        double confidence,
        List<ColumnMapping> columnMappings,
        List<String> warnings,
        List<String> unmappedSourceColumns,
        List<String> missingRequiredFields
) {
}
