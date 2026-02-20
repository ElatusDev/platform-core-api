/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.exception.CompensationNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a compensation by its identifier within the current tenant.
 */
@Service
public class GetCompensationByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final CompensationRepository compensationRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetCompensationByIdUseCase with the required dependencies.
     *
     * @param compensationRepository the repository for compensation data access
     * @param tenantContextHolder    the holder for the current tenant context
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetCompensationByIdUseCase(CompensationRepository compensationRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.compensationRepository = compensationRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a compensation by its identifier within the current tenant context.
     *
     * @param compensationId the unique identifier of the compensation
     * @return the compensation response DTO
     * @throws IllegalArgumentException          if tenant context is not available
     * @throws CompensationNotFoundException     if no compensation is found with the given identifier
     */
    public GetCompensationResponseDTO get(Long compensationId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<CompensationDataModel> queryResult = compensationRepository.findById(
                new CompensationDataModel.CompensationCompositeId(tenantId, compensationId));
        if (queryResult.isPresent()) {
            CompensationDataModel found = queryResult.get();
            return modelMapper.map(found, GetCompensationResponseDTO.class);
        } else {
            throw new CompensationNotFoundException(String.valueOf(compensationId));
        }
    }
}
