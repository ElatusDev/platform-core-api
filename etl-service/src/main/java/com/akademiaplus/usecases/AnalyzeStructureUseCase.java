/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.domain.SheetAnalysis;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.DocumentAnalysisClient;
import com.akademiaplus.util.ParsedSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Orchestrates Claude API analysis for a migration job.
 *
 * <p>Reads sample rows from MongoDB, builds prompt context, calls Claude,
 * and stores the analysis result back on the job document.</p>
 */
@Service
public class AnalyzeStructureUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeStructureUseCase.class);

    public static final String ERROR_JOB_NOT_FOUND = "Migration job not found: %s";
    public static final String ERROR_INVALID_STATUS = "Job must be in PARSED status to analyze, current status: %s";
    public static final int SAMPLE_ROW_LIMIT = 15;

    private final MigrationJobRepository jobRepository;
    private final MigrationRowRepository rowRepository;
    private final DocumentAnalysisClient analysisClient;

    /**
     * Creates a new AnalyzeStructureUseCase.
     *
     * @param jobRepository  the migration job repository
     * @param rowRepository  the migration row repository
     * @param analysisClient the Claude API analysis client
     */
    public AnalyzeStructureUseCase(MigrationJobRepository jobRepository,
                                    MigrationRowRepository rowRepository,
                                    DocumentAnalysisClient analysisClient) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.analysisClient = analysisClient;
    }

    /**
     * Triggers Claude analysis for a migration job.
     *
     * @param jobId the migration job identifier
     * @return the updated migration job with analysis results
     * @throws NoSuchElementException   if the job does not exist
     * @throws IllegalStateException    if the job is not in PARSED status
     */
    public MigrationJob execute(String jobId) {
        MigrationJob job = loadAndValidate(jobId);
        transitionToAnalyzing(job);

        try {
            List<ParsedSheet> sheets = reconstructSheets(job);
            List<SheetAnalysis> analyses = analysisClient.analyzeDocument(
                    job.getSourceFileName(), sheets, job.getEntityType());

            return storeAnalysisResult(job, analyses);
        } catch (RuntimeException e) {
            return handleAnalysisError(job, e);
        }
    }

    private MigrationJob loadAndValidate(String jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(ERROR_JOB_NOT_FOUND, jobId)));

        if (job.getStatus() != MigrationStatus.PARSED) {
            throw new IllegalStateException(
                    String.format(ERROR_INVALID_STATUS, job.getStatus()));
        }
        return job;
    }

    private void transitionToAnalyzing(MigrationJob job) {
        job.setStatus(MigrationStatus.ANALYZING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
    }

    private List<ParsedSheet> reconstructSheets(MigrationJob job) {
        List<MigrationRow> allRows = rowRepository.findByJobIdAndStatus(
                job.getId(), com.akademiaplus.domain.RowStatus.RAW);

        Map<String, List<MigrationRow>> rowsBySheet = new LinkedHashMap<>();
        for (MigrationRow row : allRows) {
            rowsBySheet.computeIfAbsent(row.getSheetName(), k -> new ArrayList<>()).add(row);
        }

        List<ParsedSheet> sheets = new ArrayList<>();
        for (Map.Entry<String, List<MigrationRow>> entry : rowsBySheet.entrySet()) {
            List<MigrationRow> sheetRows = entry.getValue();
            if (sheetRows.isEmpty()) {
                continue;
            }

            List<String> headers = new ArrayList<>(sheetRows.getFirst().getRawData().keySet());
            List<Map<String, String>> sampleRows = sheetRows.stream()
                    .limit(SAMPLE_ROW_LIMIT)
                    .map(MigrationRow::getRawData)
                    .toList();

            sheets.add(new ParsedSheet(entry.getKey(), headers, sampleRows));
        }
        return sheets;
    }

    private MigrationJob storeAnalysisResult(MigrationJob job, List<SheetAnalysis> analyses) {
        job.setDocumentAnalysis(analyses);
        job.setStatus(MigrationStatus.ANALYZED);
        job.setAnalyzedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setErrorMessage(null);
        jobRepository.save(job);

        log.info("Analysis complete — job={}, sheets={}", job.getId(), analyses.size());
        return job;
    }

    private MigrationJob handleAnalysisError(MigrationJob job, RuntimeException e) {
        log.error("Analysis failed for job={}: {}", job.getId(), e.getMessage(), e);
        job.setStatus(MigrationStatus.PARSED);
        job.setErrorMessage("Analysis failed: " + e.getMessage());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return job;
    }
}
