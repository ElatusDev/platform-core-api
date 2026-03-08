/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.exceptions.InvalidLoginException;
import com.akademiaplus.internal.exceptions.RefreshTokenExpiredException;
import com.akademiaplus.internal.exceptions.TokenReuseDetectedException;
import com.akademiaplus.internal.interfaceadapters.InternalAuthController;
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
 * Controller advice for the security module.
 * <p>
 * Extends {@link BaseControllerAdvice} for shared exception handling and
 * adds handlers for security-specific exceptions: invalid credentials,
 * expired/missing refresh tokens, and token reuse detection.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {InternalAuthController.class})
public class SecurityControllerAdvice extends BaseControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    /** Machine-readable error code for invalid login credentials. */
    public static final String CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

    /** Machine-readable error code for expired or missing refresh token. */
    public static final String CODE_REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";

    /** Machine-readable error code for refresh token reuse detection. */
    public static final String CODE_TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED";

    /**
     * Creates a new security controller advice.
     *
     * @param messageService the message service for localized messages
     */
    public SecurityControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles {@link InvalidLoginException} by returning HTTP 401 Unauthorized.
     *
     * @param ex the invalid login exception
     * @return 401 response with localized error message
     */
    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidLogin(InvalidLoginException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(messageService().getInvalidLogin());
        error.setCode(CODE_INVALID_CREDENTIALS);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link RefreshTokenExpiredException} by returning HTTP 401 Unauthorized.
     *
     * @param ex the refresh token expired exception
     * @return 401 response with error details
     */
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponseDTO> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        LOG.warn("Refresh token expired or not found: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_REFRESH_TOKEN_EXPIRED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link TokenReuseDetectedException} by returning HTTP 401 Unauthorized.
     *
     * @param ex the token reuse detected exception
     * @return 401 response with error details
     */
    @ExceptionHandler(TokenReuseDetectedException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenReuseDetected(TokenReuseDetectedException ex) {
        LOG.warn("Token reuse detected: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_TOKEN_REUSE_DETECTED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
