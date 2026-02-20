/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a payment adult student by its identifier within the current tenant.
 */
@Service
public class GetPaymentAdultStudentByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final PaymentAdultStudentRepository paymentAdultStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetPaymentAdultStudentByIdUseCase with the required dependencies.
     *
     * @param paymentAdultStudentRepository the repository for payment adult student data access
     * @param tenantContextHolder           the holder for the current tenant context
     * @param modelMapper                   the mapper for entity-to-DTO conversion
     */
    public GetPaymentAdultStudentByIdUseCase(PaymentAdultStudentRepository paymentAdultStudentRepository,
                                             TenantContextHolder tenantContextHolder,
                                             ModelMapper modelMapper) {
        this.paymentAdultStudentRepository = paymentAdultStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a payment adult student by its identifier within the current tenant context.
     *
     * @param paymentAdultStudentId the unique identifier of the payment adult student
     * @return the payment adult student response DTO
     * @throws IllegalArgumentException   if tenant context is not available
     * @throws EntityNotFoundException    if no payment adult student is found with the given identifier
     */
    public GetPaymentAdultStudentResponseDTO get(Long paymentAdultStudentId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<PaymentAdultStudentDataModel> queryResult = paymentAdultStudentRepository.findById(
                new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(tenantId, paymentAdultStudentId));
        if (queryResult.isPresent()) {
            PaymentAdultStudentDataModel found = queryResult.get();
            return modelMapper.map(found, GetPaymentAdultStudentResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.PAYMENT_ADULT_STUDENT, String.valueOf(paymentAdultStudentId));
        }
    }
}
