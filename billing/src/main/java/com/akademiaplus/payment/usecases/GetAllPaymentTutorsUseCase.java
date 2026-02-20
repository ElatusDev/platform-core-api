/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all payment tutors in the current tenant.
 */
@Service
public class GetAllPaymentTutorsUseCase {

    private final PaymentTutorRepository paymentTutorRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllPaymentTutorsUseCase with the required dependencies.
     *
     * @param paymentTutorRepository the repository for payment tutor data access
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetAllPaymentTutorsUseCase(PaymentTutorRepository paymentTutorRepository, ModelMapper modelMapper) {
        this.paymentTutorRepository = paymentTutorRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all payment tutors for the current tenant context.
     *
     * @return a list of payment tutor response DTOs
     */
    public List<GetPaymentTutorResponseDTO> getAll() {
        return paymentTutorRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetPaymentTutorResponseDTO.class))
                .toList();
    }
}
