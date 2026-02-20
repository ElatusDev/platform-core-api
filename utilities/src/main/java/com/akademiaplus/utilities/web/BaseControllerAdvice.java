/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.web;

import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import openapi.akademiaplus.domain.utilities.dto.ErrorDetailDTO;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * Abstract base exception handling for all module-specific ControllerAdvice classes.
 * <p>
 * Provides shared handlers for cross-cutting exception types that occur across
 * all modules. Module-specific ControllerAdvice classes extend this and add
 * domain-specific handlers as needed.
 * <p>
 * This class is NOT annotated with {@code @ControllerAdvice} — only concrete
 * subclasses carry that annotation with their {@code basePackageClasses}.
 *
 * @author ElatusDev
 * @since 1.0
 */
public abstract class BaseControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(BaseControllerAdvice.class);

    /** Error code for entity not found. */
    public static final String CODE_ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";

    /** Error code for deletion blocked by DB constraint. */
    public static final String CODE_DELETION_CONSTRAINT_VIOLATION = "DELETION_CONSTRAINT_VIOLATION";

    /** Error code for deletion blocked by business rule. */
    public static final String CODE_DELETION_BUSINESS_RULE = "DELETION_BUSINESS_RULE";

    /** Error code for duplicate entity (unique constraint). */
    public static final String CODE_DUPLICATE_ENTITY = "DUPLICATE_ENTITY";

    /** Error code for unclassified data integrity violation. */
    public static final String CODE_DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";

    /** Error code for bean validation errors. */
    public static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";

    /** Error code for invalid tenant context. */
    public static final String CODE_INVALID_TENANT = "INVALID_TENANT";

    /** Error code for internal server errors. */
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    private final MessageService messageService;

    /**
     * Creates the base controller advice.
     *
     * @param messageService i18n message resolution service
     */
    protected BaseControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Provides access to the message service for subclass-specific handlers.
     *
     * @return the message service instance
     */
    protected MessageService messageService() {
        return messageService;
    }

    // ── 404: Entity Not Found ──────────────────────────────────────────

    /**
     * Handles entity not found exceptions.
     *
     * @param ex the exception containing entity type and ID
     * @return HTTP 404 with error details
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                messageService.getEntityNotFound(ex.getEntityType(), ex.getEntityId()));
        error.setCode(CODE_ENTITY_NOT_FOUND);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ── 409: Deletion Not Allowed (constraint or business rule) ────────

    /**
     * Handles entity deletion not allowed exceptions.
     *
     * @param ex the exception containing entity type, ID, and optional reason
     * @return HTTP 409 with error details
     */
    @ExceptionHandler(EntityDeletionNotAllowedException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeletionNotAllowed(
            EntityDeletionNotAllowedException ex) {
        ErrorResponseDTO error;
        if (ex.getReason() != null) {
            error = new ErrorResponseDTO(
                    messageService.getEntityDeleteNotAllowedWithReason(
                            ex.getEntityType(), ex.getEntityId(), ex.getReason()));
            error.setCode(CODE_DELETION_BUSINESS_RULE);
        } else {
            error = new ErrorResponseDTO(
                    messageService.getEntityDeleteNotAllowed(ex.getEntityType()));
            error.setCode(CODE_DELETION_CONSTRAINT_VIOLATION);
        }
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 409: Duplicate Entity (unique constraint on create/update) ─────

    /**
     * Handles duplicate entity exceptions.
     *
     * @param ex the exception containing entity type and duplicate field
     * @return HTTP 409 with error details
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateEntity(DuplicateEntityException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                messageService.getEntityDuplicateField(ex.getEntityType(), ex.getField()));
        error.setCode(CODE_DUPLICATE_ENTITY);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 409: Unclassified DataIntegrityViolation (fallback) ────────────

    /**
     * Handles unclassified data integrity violations.
     *
     * @param ex the Spring data integrity violation exception
     * @return HTTP 409 with generic constraint message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(messageService.getConstraintViolation());
        error.setCode(CODE_DATA_INTEGRITY_VIOLATION);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 400: Bean Validation ───────────────────────────────────────────

    /**
     * Handles bean validation errors from request body binding.
     *
     * @param ex the validation exception with field errors
     * @return HTTP 400 with validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex) {
        List<ErrorDetailDTO> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    ErrorDetailDTO d = new ErrorDetailDTO();
                    d.setField(fe.getField());
                    d.setMessage(fe.getDefaultMessage());
                    return d;
                })
                .toList();
        ErrorResponseDTO error = new ErrorResponseDTO("Error de validacion en la solicitud");
        error.setCode(CODE_VALIDATION_ERROR);
        error.setDetails(details);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ── 400: Invalid Tenant Context ────────────────────────────────────

    /**
     * Handles invalid tenant context exceptions.
     *
     * @param ex the invalid tenant exception
     * @return HTTP 400 with tenant error message
     */
    @ExceptionHandler(InvalidTenantException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTenant(InvalidTenantException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(messageService.getInvalidTenant());
        error.setCode(CODE_INVALID_TENANT);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ── 500: Encryption/Decryption ─────────────────────────────────────

    /**
     * Handles encryption and decryption failure exceptions.
     *
     * @param ex the crypto failure exception
     * @return HTTP 500 with internal error message
     */
    @ExceptionHandler({EncryptionFailureException.class, DecryptionFailureException.class})
    public ResponseEntity<ErrorResponseDTO> handleCryptoFailure(RuntimeException ex) {
        LOG.error("Cryptographic operation failed", ex);
        ErrorResponseDTO error = new ErrorResponseDTO(messageService.getInternalErrorHighSeverity());
        error.setCode(CODE_INTERNAL_ERROR);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── 500: Unhandled Fallback ────────────────────────────────────────

    /**
     * Catches all unhandled exceptions as a last resort.
     *
     * @param ex the unhandled exception
     * @return HTTP 500 with internal error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex) {
        LOG.error("Unhandled exception", ex);
        ErrorResponseDTO error = new ErrorResponseDTO(messageService.getInternalErrorHighSeverity());
        error.setCode(CODE_INTERNAL_ERROR);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
