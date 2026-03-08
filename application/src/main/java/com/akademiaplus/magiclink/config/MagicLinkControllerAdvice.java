/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.config;

import com.akademiaplus.magiclink.exceptions.MagicLinkTokenAlreadyUsedException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenExpiredException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenNotFoundException;
import com.akademiaplus.magiclink.interfaceadapters.MagicLinkController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for magic link authentication endpoints.
 *
 * <p>Maps magic link token validation failures to HTTP 401 responses.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = MagicLinkController.class)
public class MagicLinkControllerAdvice extends BaseControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(MagicLinkControllerAdvice.class);

    /** Error code for token not found. */
    public static final String CODE_TOKEN_NOT_FOUND = MagicLinkTokenNotFoundException.ERROR_CODE;

    /** Error code for token expired. */
    public static final String CODE_TOKEN_EXPIRED = MagicLinkTokenExpiredException.ERROR_CODE;

    /** Error code for token already used. */
    public static final String CODE_TOKEN_ALREADY_USED = MagicLinkTokenAlreadyUsedException.ERROR_CODE;

    /**
     * Constructs the controller advice with the message service.
     *
     * @param messageService the i18n message resolution service
     */
    public MagicLinkControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles magic link token not found.
     *
     * @param ex the exception
     * @return HTTP 401 with error details
     */
    @ExceptionHandler(MagicLinkTokenNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenNotFound(MagicLinkTokenNotFoundException ex) {
        LOG.warn("Magic link token not found: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_TOKEN_NOT_FOUND);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles magic link token expired.
     *
     * @param ex the exception
     * @return HTTP 401 with error details
     */
    @ExceptionHandler(MagicLinkTokenExpiredException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenExpired(MagicLinkTokenExpiredException ex) {
        LOG.warn("Magic link token expired: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_TOKEN_EXPIRED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles magic link token already used.
     *
     * @param ex the exception
     * @return HTTP 401 with error details
     */
    @ExceptionHandler(MagicLinkTokenAlreadyUsedException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenAlreadyUsed(MagicLinkTokenAlreadyUsedException ex) {
        LOG.warn("Magic link token already used: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_TOKEN_ALREADY_USED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
