/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a payment tutor by its identifier within the current tenant.
 */
@Service
public class GetPaymentTutorByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final PaymentTutorRepository paymentTutorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetPaymentTutorByIdUseCase with the required dependencies.
     *
     * @param paymentTutorRepository the repository for payment tutor data access
     * @param tenantContextHolder    the holder for the current tenant context
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetPaymentTutorByIdUseCase(PaymentTutorRepository paymentTutorRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.paymentTutorRepository = paymentTutorRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a payment tutor by its identifier within the current tenant context.
     *
     * @param paymentTutorId the unique identifier of the payment tutor
     * @return the payment tutor response DTO
     * @throws IllegalArgumentException   if tenant context is not available
     * @throws EntityNotFoundException    if no payment tutor is found with the given identifier
     */
    public GetPaymentTutorResponseDTO get(Long paymentTutorId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<PaymentTutorDataModel> queryResult = paymentTutorRepository.findById(
                new PaymentTutorDataModel.PaymentTutorCompositeId(tenantId, paymentTutorId));
        if (queryResult.isPresent()) {
            PaymentTutorDataModel found = queryResult.get();
            return modelMapper.map(found, GetPaymentTutorResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.PAYMENT_TUTOR, String.valueOf(paymentTutorId));
        }
    }
}
