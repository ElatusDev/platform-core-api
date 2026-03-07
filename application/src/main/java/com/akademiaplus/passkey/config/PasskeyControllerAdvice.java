/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.config;

import com.akademiaplus.passkey.exceptions.PasskeyAuthenticationException;
import com.akademiaplus.passkey.exceptions.PasskeyRegistrationException;
import com.akademiaplus.passkey.interfaceadapters.PasskeyController;
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
 * Exception handler for passkey-related controller endpoints.
 *
 * <p>Handles {@link PasskeyRegistrationException} (400 Bad Request) and
 * {@link PasskeyAuthenticationException} (401 Unauthorized) in addition to
 * all cross-cutting exceptions inherited from {@link BaseControllerAdvice}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = PasskeyController.class)
public class PasskeyControllerAdvice extends BaseControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(PasskeyControllerAdvice.class);

    /** Error code for passkey registration failure. */
    public static final String CODE_PASSKEY_REGISTRATION_FAILED = "PASSKEY_REGISTRATION_FAILED";

    /** Error code for passkey authentication failure. */
    public static final String CODE_PASSKEY_AUTHENTICATION_FAILED = "PASSKEY_AUTHENTICATION_FAILED";

    /**
     * Constructs the controller advice with the message service.
     *
     * @param messageService the i18n message resolution service
     */
    public PasskeyControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles passkey registration failures.
     *
     * @param ex the registration exception
     * @return HTTP 400 with error details
     */
    @ExceptionHandler(PasskeyRegistrationException.class)
    public ResponseEntity<ErrorResponseDTO> handleRegistrationFailure(PasskeyRegistrationException ex) {
        LOG.warn("Passkey registration failed: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_PASSKEY_REGISTRATION_FAILED);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles passkey authentication failures.
     *
     * @param ex the authentication exception
     * @return HTTP 401 with error details
     */
    @ExceptionHandler(PasskeyAuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationFailure(PasskeyAuthenticationException ex) {
        LOG.warn("Passkey authentication failed: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_PASSKEY_AUTHENTICATION_FAILED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
