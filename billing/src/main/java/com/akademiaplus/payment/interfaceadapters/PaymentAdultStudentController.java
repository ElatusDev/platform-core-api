/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.interfaceadapters;

import com.akademiaplus.payment.usecases.DeletePaymentAdultStudentUseCase;
import com.akademiaplus.payment.usecases.GetAllPaymentAdultStudentsUseCase;
import com.akademiaplus.payment.usecases.GetPaymentAdultStudentByIdUseCase;
import com.akademiaplus.payment.usecases.PaymentAdultStudentCreationUseCase;
import openapi.akademiaplus.domain.billing.api.PaymentAdultStudentsApi;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for payment-adult-student operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class PaymentAdultStudentController implements PaymentAdultStudentsApi {

    private final PaymentAdultStudentCreationUseCase paymentAdultStudentCreationUseCase;
    private final GetAllPaymentAdultStudentsUseCase getAllPaymentAdultStudentsUseCase;
    private final GetPaymentAdultStudentByIdUseCase getPaymentAdultStudentByIdUseCase;
    private final DeletePaymentAdultStudentUseCase deletePaymentAdultStudentUseCase;

    public PaymentAdultStudentController(
            PaymentAdultStudentCreationUseCase paymentAdultStudentCreationUseCase,
            GetAllPaymentAdultStudentsUseCase getAllPaymentAdultStudentsUseCase,
            GetPaymentAdultStudentByIdUseCase getPaymentAdultStudentByIdUseCase,
            DeletePaymentAdultStudentUseCase deletePaymentAdultStudentUseCase) {
        this.paymentAdultStudentCreationUseCase = paymentAdultStudentCreationUseCase;
        this.getAllPaymentAdultStudentsUseCase = getAllPaymentAdultStudentsUseCase;
        this.getPaymentAdultStudentByIdUseCase = getPaymentAdultStudentByIdUseCase;
        this.deletePaymentAdultStudentUseCase = deletePaymentAdultStudentUseCase;
    }

    @Override
    public ResponseEntity<PaymentAdultStudentCreationResponseDTO> createPaymentAdultStudent(
            PaymentAdultStudentCreationRequestDTO paymentAdultStudentCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentAdultStudentCreationUseCase.create(paymentAdultStudentCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetPaymentAdultStudentResponseDTO>> getPaymentAdultStudents() {
        return ResponseEntity.ok(getAllPaymentAdultStudentsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetPaymentAdultStudentResponseDTO> getPaymentAdultStudentById(
            Long paymentAdultStudentId) {
        return ResponseEntity.ok(getPaymentAdultStudentByIdUseCase.get(paymentAdultStudentId));
    }

    @Override
    public ResponseEntity<Void> deletePaymentAdultStudent(Long paymentAdultStudentId) {
        deletePaymentAdultStudentUseCase.delete(paymentAdultStudentId);
        return ResponseEntity.noContent().build();
    }
}
