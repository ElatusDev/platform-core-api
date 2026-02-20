/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.interfaceadapters;

import com.akademiaplus.payment.usecases.DeletePaymentTutorUseCase;
import com.akademiaplus.payment.usecases.GetAllPaymentTutorsUseCase;
import com.akademiaplus.payment.usecases.GetPaymentTutorByIdUseCase;
import com.akademiaplus.payment.usecases.PaymentTutorCreationUseCase;
import openapi.akademiaplus.domain.billing.api.PaymentTutorsApi;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for payment-tutor operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class PaymentTutorController implements PaymentTutorsApi {

    private final PaymentTutorCreationUseCase paymentTutorCreationUseCase;
    private final GetAllPaymentTutorsUseCase getAllPaymentTutorsUseCase;
    private final GetPaymentTutorByIdUseCase getPaymentTutorByIdUseCase;
    private final DeletePaymentTutorUseCase deletePaymentTutorUseCase;

    public PaymentTutorController(
            PaymentTutorCreationUseCase paymentTutorCreationUseCase,
            GetAllPaymentTutorsUseCase getAllPaymentTutorsUseCase,
            GetPaymentTutorByIdUseCase getPaymentTutorByIdUseCase,
            DeletePaymentTutorUseCase deletePaymentTutorUseCase) {
        this.paymentTutorCreationUseCase = paymentTutorCreationUseCase;
        this.getAllPaymentTutorsUseCase = getAllPaymentTutorsUseCase;
        this.getPaymentTutorByIdUseCase = getPaymentTutorByIdUseCase;
        this.deletePaymentTutorUseCase = deletePaymentTutorUseCase;
    }

    @Override
    public ResponseEntity<PaymentTutorCreationResponseDTO> createPaymentTutor(
            PaymentTutorCreationRequestDTO paymentTutorCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentTutorCreationUseCase.create(paymentTutorCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetPaymentTutorResponseDTO>> getPaymentTutors() {
        return ResponseEntity.ok(getAllPaymentTutorsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetPaymentTutorResponseDTO> getPaymentTutorById(
            Long paymentTutorId) {
        return ResponseEntity.ok(getPaymentTutorByIdUseCase.get(paymentTutorId));
    }

    @Override
    public ResponseEntity<Void> deletePaymentTutor(Long paymentTutorId) {
        deletePaymentTutorUseCase.delete(paymentTutorId);
        return ResponseEntity.noContent().build();
    }
}
