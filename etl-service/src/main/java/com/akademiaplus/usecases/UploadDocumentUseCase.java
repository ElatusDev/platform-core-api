/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationEntityType;
import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.ExcelParser;
import com.akademiaplus.util.ParsedSheet;
import com.akademiaplus.util.WordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles file upload, parsing, and initial staging in MongoDB.
 *
 * <p>Accepts Excel (.xlsx) or Word (.docx) files, parses them into rows,
 * and stores the job and rows in MongoDB for further processing.</p>
 */
@Service
public class UploadDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadDocumentUseCase.class);

    public static final String ERROR_FILE_EMPTY = "Uploaded file is empty";
    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported file format. Only .xlsx and .docx files are accepted";
    public static final String ERROR_DUPLICATE_FILE = "A migration job already exists for file '%s' in this tenant";
    public static final String ERROR_PARSE_FAILED = "Failed to parse file";

    public static final String EXTENSION_XLSX = ".xlsx";
    public static final String EXTENSION_XLS = ".xls";
    public static final String EXTENSION_DOCX = ".docx";

    private final MigrationJobRepository jobRepository;
    private final MigrationRowRepository rowRepository;
    private final ExcelParser excelParser;
    private final WordParser wordParser;

    /**
     * Creates a new UploadDocumentUseCase.
     *
     * @param jobRepository the migration job repository
     * @param rowRepository the migration row repository
     * @param excelParser   the Excel file parser
     * @param wordParser    the Word file parser
     */
    public UploadDocumentUseCase(MigrationJobRepository jobRepository,
                                 MigrationRowRepository rowRepository,
                                 ExcelParser excelParser,
                                 WordParser wordParser) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.excelParser = excelParser;
        this.wordParser = wordParser;
    }

    /**
     * Uploads and parses a document, creating a migration job and raw rows in MongoDB.
     *
     * @param file       the uploaded file
     * @param entityType optional entity type hint (may be null for auto-detection)
     * @param tenantId   the current tenant identifier
     * @param createdBy  the user who initiated the upload
     * @return the created migration job
     * @throws IllegalArgumentException if the file is invalid, unsupported, or a duplicate
     */
    public MigrationJob execute(MultipartFile file, MigrationEntityType entityType,
                                Long tenantId, String createdBy) {
        validateFile(file);
        String fileName = file.getOriginalFilename();
        checkDuplicate(tenantId, fileName);

        List<ParsedSheet> sheets = parseFile(file);
        MigrationJob job = createJob(file, entityType, tenantId, createdBy, sheets);
        List<MigrationRow> rows = createRows(job.getId(), sheets);

        job.setTotalRows(rows.size());
        jobRepository.save(job);

        log.info("Upload complete — job={}, tenant={}, file={}, rows={}",
                job.getId(), tenantId, fileName, rows.size());
        return job;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ERROR_FILE_EMPTY);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isSupportedFormat(fileName)) {
            throw new IllegalArgumentException(ERROR_UNSUPPORTED_FORMAT);
        }
    }

    private boolean isSupportedFormat(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(EXTENSION_XLSX)
                || lower.endsWith(EXTENSION_XLS)
                || lower.endsWith(EXTENSION_DOCX);
    }

    private void checkDuplicate(Long tenantId, String fileName) {
        jobRepository.findByTenantIdAndSourceFileNameAndStatusNot(
                tenantId, fileName, MigrationStatus.FAILED
        ).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    String.format(ERROR_DUPLICATE_FILE, fileName));
        });
    }

    private List<ParsedSheet> parseFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            if (fileName != null && fileName.toLowerCase().endsWith(EXTENSION_DOCX)) {
                return wordParser.parse(file.getInputStream(), fileName);
            }
            return excelParser.parse(file.getInputStream(), fileName);
        } catch (IOException e) {
            throw new IllegalArgumentException(ERROR_PARSE_FAILED + ": " + fileName, e);
        }
    }

    private MigrationJob createJob(MultipartFile file, MigrationEntityType entityType,
                                   Long tenantId, String createdBy,
                                   List<ParsedSheet> sheets) {
        Instant now = Instant.now();
        MigrationJob job = MigrationJob.builder()
                .tenantId(tenantId)
                .entityType(entityType)
                .sourceFileName(file.getOriginalFilename())
                .sourceFileSize(file.getSize())
                .status(MigrationStatus.PARSED)
                .totalRows(0)
                .validRows(0)
                .errorRows(0)
                .loadedRows(0)
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return jobRepository.save(job);
    }

    private List<MigrationRow> createRows(String jobId, List<ParsedSheet> sheets) {
        List<MigrationRow> allRows = new ArrayList<>();
        for (ParsedSheet sheet : sheets) {
            for (int i = 0; i < sheet.rows().size(); i++) {
                MigrationRow row = MigrationRow.builder()
                        .jobId(jobId)
                        .sheetName(sheet.name())
                        .rowNumber(i + 1)
                        .status(RowStatus.RAW)
                        .rawData(sheet.rows().get(i))
                        .build();
                allRows.add(row);
            }
        }
        if (!allRows.isEmpty()) {
            rowRepository.saveAll(allRows);
        }
        return allRows;
    }
}
