/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.event.interfaceadapters.CourseEventController;
import com.akademiaplus.exception.CollaboratorNotFoundException;
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.program.interfaceadapters.CourseController;
import com.akademiaplus.program.interfaceadapters.ScheduleController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for handling course management module exceptions.
 * Extends BaseControllerAdvice to inherit generic exception handling.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {CourseController.class, ScheduleController.class,
        CourseEventController.class})
public class CoordinationControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs a new CoordinationControllerAdvice with the required MessageService.
     *
     * @param messageService the message service for internationalized error messages
     */
    public CoordinationControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles ScheduleNotAvailableException when a schedule is already assigned to a course.
     *
     * @param ex the exception containing schedule conflict details
     * @return ResponseEntity with error details and HTTP 409 CONFLICT status
     */
    @ExceptionHandler(ScheduleNotAvailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduleConflict(
            ScheduleNotAvailableException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                messageService().getScheduleNotAvailable(ex.getMessage()));
        error.setCode("SCHEDULE_CONFLICT");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handles CollaboratorNotFoundException when a collaborator is not found.
     *
     * @param ex the exception containing missing collaborator details
     * @return ResponseEntity with error details and HTTP 400 BAD REQUEST status
     */
    @ExceptionHandler(CollaboratorNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleCollaboratorNotFound(
            CollaboratorNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                messageService().getCourseCollaboratorNotFound(ex.getMessage()));
        error.setCode("COLLABORATOR_NOT_FOUND");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
