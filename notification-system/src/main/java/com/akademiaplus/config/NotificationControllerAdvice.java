/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.exception.NotificationNotFoundException;
import com.akademiaplus.notification.interfaceadapters.NotificationController;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for notification system exception handling.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {NotificationController.class})
public class NotificationControllerAdvice {

    private final MessageService messageService;

    public NotificationControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleNotificationNotFoundException(
            NotificationNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getNotificationNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }
}
