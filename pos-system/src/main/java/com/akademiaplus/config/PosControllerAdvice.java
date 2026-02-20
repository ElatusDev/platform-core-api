/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.exception.StoreProductNotFoundException;
import com.akademiaplus.exception.StoreTransactionNotFoundException;
import com.akademiaplus.store.interfaceadapters.StoreProductController;
import com.akademiaplus.store.interfaceadapters.StoreTransactionController;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for POS system exception handling.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {StoreProductController.class,
        StoreTransactionController.class})
public class PosControllerAdvice {

    private final MessageService messageService;

    public PosControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleStoreProductNotFoundException(
            StoreProductNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getStoreProductNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleStoreTransactionNotFoundException(
            StoreTransactionNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getStoreTransactionNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }
}
