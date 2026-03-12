/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.ColumnMapping;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.TransformExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies confirmed column mappings and transforms to all RAW rows of a migration job.
 *
 * <p>Processes rows in batches of {@value BATCH_SIZE} to manage memory.
 * For each row, applies all confirmed column mappings via {@link TransformExecutor}
 * and stores the result in the row's {@code mappedData} field.</p>
 */
@Service
public class ApplyMappingUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyMappingUseCase.class);

    public static final int BATCH_SIZE = 100;

    private final MigrationRowRepository rowRepository;
    private final TransformExecutor transformExecutor;

    /**
     * Creates a new ApplyMappingUseCase.
     *
     * @param rowRepository     the migration row repository
     * @param transformExecutor the transform executor
     */
    public ApplyMappingUseCase(MigrationRowRepository rowRepository,
                                TransformExecutor transformExecutor) {
        this.rowRepository = rowRepository;
        this.transformExecutor = transformExecutor;
    }

    /**
     * Applies column mappings to all RAW rows of a migration job.
     *
     * @param jobId    the migration job identifier
     * @param mappings the confirmed column mappings to apply
     * @return the number of rows successfully mapped
     */
    public int execute(String jobId, List<ColumnMapping> mappings) {
        List<MigrationRow> rawRows = rowRepository.findByJobIdAndStatus(jobId, RowStatus.RAW);
        int mapped = 0;

        for (int i = 0; i < rawRows.size(); i += BATCH_SIZE) {
            List<MigrationRow> batch = rawRows.subList(i, Math.min(i + BATCH_SIZE, rawRows.size()));
            mapped += processBatch(batch, mappings);
        }

        log.info("Mapping complete — job={}, mapped={}/{}", jobId, mapped, rawRows.size());
        return mapped;
    }

    private int processBatch(List<MigrationRow> batch, List<ColumnMapping> mappings) {
        int mapped = 0;
        for (MigrationRow row : batch) {
            Map<String, String> mappedData = applyMappings(row.getRawData(), mappings);
            row.setMappedData(mappedData);
            row.setStatus(RowStatus.MAPPED);
            mapped++;
        }
        rowRepository.saveAll(batch);
        return mapped;
    }

    private Map<String, String> applyMappings(Map<String, String> rawData,
                                               List<ColumnMapping> mappings) {
        Map<String, String> result = new HashMap<>();
        for (ColumnMapping mapping : mappings) {
            String sourceValue = rawData.getOrDefault(mapping.source(), "");
            Map<String, String> transformed = transformExecutor.apply(
                    sourceValue, mapping.transform(), mapping.target());
            result.putAll(transformed);
        }
        return result;
    }
}
