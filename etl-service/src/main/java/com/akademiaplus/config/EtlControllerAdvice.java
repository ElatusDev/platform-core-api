/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.interfaceadapters.MigrationController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

/**
 * Controller advice for ETL migration exception handling.
 *
 * <p>Extends {@link BaseControllerAdvice} for shared exception handling
 * and adds ETL-specific handlers for file upload, state machine, and
 * validation errors.</p>
 */
@ControllerAdvice(basePackageClasses = {MigrationController.class})
public class EtlControllerAdvice extends BaseControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(EtlControllerAdvice.class);

    public static final String CODE_INVALID_STATE = "INVALID_STATE";
    public static final String CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String CODE_JOB_NOT_FOUND = "JOB_NOT_FOUND";
    public static final String CODE_FILE_TOO_LARGE = "FILE_TOO_LARGE";

    /**
     * Constructs a new EtlControllerAdvice.
     *
     * @param messageService the service for retrieving localized messages
     */
    public EtlControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles job not found errors (invalid jobId).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NoSuchElementException ex) {
        log.warn("ETL job not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(CODE_JOB_NOT_FOUND, ex.getMessage()));
    }

    /**
     * Handles invalid state transitions (e.g., loading a non-VALIDATED job).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        log.warn("ETL invalid state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(CODE_INVALID_STATE, ex.getMessage()));
    }

    /**
     * Handles invalid arguments (missing required fields, no valid rows, etc.).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("ETL invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(CODE_INVALID_ARGUMENT, ex.getMessage()));
    }

    /**
     * Handles file upload size exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("ETL file too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(CODE_FILE_TOO_LARGE, "File exceeds maximum upload size of 10MB"));
    }

    private ErrorResponseDTO buildError(String code, String message) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setCode(code);
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        return error;
    }
}
