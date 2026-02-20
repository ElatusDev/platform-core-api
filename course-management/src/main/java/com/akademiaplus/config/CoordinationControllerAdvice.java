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
import com.akademiaplus.exception.CourseEventNotFoundException;
import com.akademiaplus.exception.CourseNotFoundException;
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.exception.ScheduleNotFoundException;
import com.akademiaplus.program.interfaceadapters.CourseController;
import com.akademiaplus.program.interfaceadapters.ScheduleController;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackageClasses= {CourseController.class, ScheduleController.class,
                    CourseEventController.class})
public class CoordinationControllerAdvice {
    private final MessageService messageService;

    public CoordinationControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleScheduleNotAvailableException(ScheduleNotAvailableException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getScheduleNotAvailable(ex.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleCollaboratorNotFoundException(CollaboratorNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getCourseCollaboratorNotFound(ex.getMessage())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleScheduleNotFoundException(ScheduleNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getScheduleNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleCourseNotFoundException(CourseNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getCourseNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleCourseEventNotFoundException(CourseEventNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getCourseEventNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

}
