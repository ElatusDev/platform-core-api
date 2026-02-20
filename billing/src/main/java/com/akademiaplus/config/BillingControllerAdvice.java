/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.exception.*;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentController;
import com.akademiaplus.membership.interfaceadapters.MembershipController;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorController;
import com.akademiaplus.payment.interfaceadapters.PaymentAdultStudentController;
import com.akademiaplus.payment.interfaceadapters.PaymentTutorController;
import com.akademiaplus.payroll.interfaceadapters.CompensationController;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for billing module exception handling.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {MembershipController.class,
        MembershipAdultStudentController.class, MembershipTutorController.class,
        PaymentAdultStudentController.class, PaymentTutorController.class,
        CompensationController.class})
public class BillingControllerAdvice {

    private final MessageService messageService;

    public BillingControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleMembershipNotFoundException(MembershipNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(messageService.getMembershipNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleMembershipAdultStudentNotFoundException(
            MembershipAdultStudentNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getMembershipAdultStudentNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleMembershipTutorNotFoundException(
            MembershipTutorNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getMembershipTutorNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handlePaymentAdultStudentNotFoundException(
            PaymentAdultStudentNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getPaymentAdultStudentNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handlePaymentTutorNotFoundException(
            PaymentTutorNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getPaymentTutorNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDTO> handleCompensationNotFoundException(
            CompensationNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponseDTO(
                messageService.getCompensationNotFound(ex.getMessage())), HttpStatus.NOT_FOUND);
    }
}
