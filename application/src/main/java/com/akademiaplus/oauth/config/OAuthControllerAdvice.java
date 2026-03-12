/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.config;

import com.akademiaplus.oauth.exceptions.OAuthProviderException;
import com.akademiaplus.oauth.exceptions.UnsupportedProviderException;
import com.akademiaplus.oauth.interfaceadapters.OAuthLoginController;
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
 * Exception handler for OAuth authentication endpoints.
 *
 * <p>Maps OAuth-specific exceptions to appropriate HTTP responses.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = OAuthLoginController.class)
public class OAuthControllerAdvice extends BaseControllerAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthControllerAdvice.class);

    /** Error code for unsupported provider. */
    public static final String CODE_UNSUPPORTED_PROVIDER = UnsupportedProviderException.ERROR_CODE;

    /** Error code for provider communication failure. */
    public static final String CODE_OAUTH_PROVIDER_ERROR = OAuthProviderException.ERROR_CODE;

    /**
     * Constructs the controller advice with the message service.
     *
     * @param messageService the i18n message resolution service
     */
    public OAuthControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles unsupported OAuth provider.
     *
     * @param ex the exception
     * @return HTTP 400 with error details
     */
    @ExceptionHandler(UnsupportedProviderException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnsupportedProvider(UnsupportedProviderException ex) {
        LOG.warn("Unsupported OAuth provider: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_UNSUPPORTED_PROVIDER);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles OAuth provider communication failure.
     *
     * @param ex the exception
     * @return HTTP 401 with error details
     */
    @ExceptionHandler(OAuthProviderException.class)
    public ResponseEntity<ErrorResponseDTO> handleOAuthProviderError(OAuthProviderException ex) {
        LOG.warn("OAuth provider error: {}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_OAUTH_PROVIDER_ERROR);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
