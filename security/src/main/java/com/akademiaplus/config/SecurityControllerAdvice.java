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
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackageClasses= {InternalAuthController.class})
public class SecurityControllerAdvice {
    private final MessageService messageService;

    public SecurityControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler({InvalidLoginException.class})
    public ResponseEntity<ErrorResponseDTO>  handleInvalidLoginException() {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getInvalidLogin()), HttpStatus.BAD_REQUEST);
    }

}
