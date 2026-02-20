/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.exceptions.InvalidLoginException;
import com.akademiaplus.internal.interfaceadapters.InternalAuthController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for the security module.
 * <p>
 * Extends {@link BaseControllerAdvice} for shared exception handling and
 * retains the {@link InvalidLoginException} handler with HTTP 401 Unauthorized
 * per RFC 9110 (authentication failure, not malformed request).
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {InternalAuthController.class})
public class SecurityControllerAdvice extends BaseControllerAdvice {

    /** Machine-readable error code for invalid login credentials. */
    public static final String CODE_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

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
}
