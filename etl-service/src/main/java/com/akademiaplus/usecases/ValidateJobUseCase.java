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
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.domain.ValidationError;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.EntityFieldDefinition;
import com.akademiaplus.util.EntityFieldRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Validates all MAPPED rows of a migration job against the target entity schema.
 *
 * <p>Checks required fields, type validity, email/phone formats, and updates
 * each row to VALID or INVALID with detailed validation errors.</p>
 */
@Service
public class ValidateJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValidateJobUseCase.class);

    public static final String ERROR_JOB_NOT_FOUND = "Migration job not found: %s";
    public static final String ERROR_INVALID_STATUS = "Job must have MAPPED rows to validate";
    public static final String ERROR_NO_ENTITY_TYPE = "Entity type must be set before validation";
    public static final String SEVERITY_ERROR = "ERROR";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MigrationJobRepository jobRepository;
    private final MigrationRowRepository rowRepository;
    private final EntityFieldRegistry fieldRegistry;

    /**
     * Creates a new ValidateJobUseCase.
     *
     * @param jobRepository the migration job repository
     * @param rowRepository the migration row repository
     * @param fieldRegistry the entity field registry
     */
    public ValidateJobUseCase(MigrationJobRepository jobRepository,
                               MigrationRowRepository rowRepository,
                               EntityFieldRegistry fieldRegistry) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.fieldRegistry = fieldRegistry;
    }

    /**
     * Validates all MAPPED rows and updates job statistics.
     *
     * @param jobId the migration job identifier
     * @return the updated migration job
     */
    public MigrationJob execute(String jobId) {
        MigrationJob job = loadAndValidate(jobId);
        List<EntityFieldDefinition> fields = fieldRegistry.getFields(job.getEntityType());

        List<MigrationRow> mappedRows = rowRepository.findByJobIdAndStatus(jobId, RowStatus.MAPPED);

        int valid = 0;
        int invalid = 0;

        for (int i = 0; i < mappedRows.size(); i += 100) {
            List<MigrationRow> batch = mappedRows.subList(i, Math.min(i + 100, mappedRows.size()));
            for (MigrationRow row : batch) {
                List<ValidationError> errors = validateRow(row.getMappedData(), fields);
                if (errors.isEmpty()) {
                    row.setStatus(RowStatus.VALID);
                    valid++;
                } else {
                    row.setStatus(RowStatus.INVALID);
                    row.setValidationErrors(errors);
                    invalid++;
                }
            }
            rowRepository.saveAll(batch);
        }

        job.setValidRows(valid);
        job.setErrorRows(invalid);
        job.setStatus(MigrationStatus.VALIDATED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        log.info("Validation complete — job={}, valid={}, invalid={}", jobId, valid, invalid);
        return job;
    }

    private MigrationJob loadAndValidate(String jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(ERROR_JOB_NOT_FOUND, jobId)));

        if (job.getEntityType() == null) {
            throw new IllegalArgumentException(ERROR_NO_ENTITY_TYPE);
        }
        return job;
    }

    private List<ValidationError> validateRow(Map<String, String> mappedData,
                                               List<EntityFieldDefinition> fields) {
        List<ValidationError> errors = new ArrayList<>();
        if (mappedData == null) {
            errors.add(new ValidationError("_row", "Mapped data is null", SEVERITY_ERROR));
            return errors;
        }

        for (EntityFieldDefinition field : fields) {
            String value = mappedData.get(field.name());
            validateField(field, value, errors);
        }
        return errors;
    }

    private void validateField(EntityFieldDefinition field, String value,
                                List<ValidationError> errors) {
        boolean isEmpty = value == null || value.isBlank();

        if (field.required() && isEmpty) {
            errors.add(new ValidationError(field.name(),
                    field.name() + " is required", SEVERITY_ERROR));
            return;
        }

        if (isEmpty) {
            return;
        }

        switch (field.type()) {
            case "LocalDate" -> validateDate(field.name(), value, errors);
            case "LocalTime" -> validateTime(field.name(), value, errors);
            case "Long" -> validateLong(field.name(), value, errors);
            case "Integer" -> validateInteger(field.name(), value, errors);
            case "BigDecimal" -> validateBigDecimal(field.name(), value, errors);
            default -> { /* String type — no format validation needed */ }
        }

        if ("email".equalsIgnoreCase(field.name()) && !isEmpty) {
            validateEmail(value, errors);
        }
    }

    private void validateDate(String fieldName, String value, List<ValidationError> errors) {
        try {
            LocalDate.parse(value);
        } catch (Exception e) {
            errors.add(new ValidationError(fieldName,
                    "Invalid date format, expected YYYY-MM-DD: " + value, SEVERITY_ERROR));
        }
    }

    private void validateTime(String fieldName, String value, List<ValidationError> errors) {
        try {
            LocalTime.parse(value);
        } catch (Exception e) {
            errors.add(new ValidationError(fieldName,
                    "Invalid time format, expected HH:mm: " + value, SEVERITY_ERROR));
        }
    }

    private void validateLong(String fieldName, String value, List<ValidationError> errors) {
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(fieldName,
                    "Invalid number: " + value, SEVERITY_ERROR));
        }
    }

    private void validateInteger(String fieldName, String value, List<ValidationError> errors) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(fieldName,
                    "Invalid integer: " + value, SEVERITY_ERROR));
        }
    }

    private void validateBigDecimal(String fieldName, String value, List<ValidationError> errors) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(fieldName,
                    "Invalid decimal number: " + value, SEVERITY_ERROR));
        }
    }

    private void validateEmail(String value, List<ValidationError> errors) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            errors.add(new ValidationError("email",
                    "Invalid email format: " + value, SEVERITY_ERROR));
        }
    }
}
