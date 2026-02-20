/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all payment adult students in the current tenant.
 */
@Service
public class GetAllPaymentAdultStudentsUseCase {

    private final PaymentAdultStudentRepository paymentAdultStudentRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllPaymentAdultStudentsUseCase with the required dependencies.
     *
     * @param paymentAdultStudentRepository the repository for payment adult student data access
     * @param modelMapper                   the mapper for entity-to-DTO conversion
     */
    public GetAllPaymentAdultStudentsUseCase(PaymentAdultStudentRepository paymentAdultStudentRepository, ModelMapper modelMapper) {
        this.paymentAdultStudentRepository = paymentAdultStudentRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all payment adult students for the current tenant context.
     *
     * @return a list of payment adult student response DTOs
     */
    public List<GetPaymentAdultStudentResponseDTO> getAll() {
        return paymentAdultStudentRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetPaymentAdultStudentResponseDTO.class))
                .toList();
    }
}
