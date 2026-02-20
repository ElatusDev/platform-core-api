/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.exception.MinorStudentNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a minor student by their identifier within the current tenant.
 */
@Service
public class GetMinorStudentByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetMinorStudentByIdUseCase with the required dependencies.
     *
     * @param minorStudentRepository the repository for minor student data access
     * @param tenantContextHolder    the holder for the current tenant context
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetMinorStudentByIdUseCase(MinorStudentRepository minorStudentRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.minorStudentRepository = minorStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a minor student by their identifier within the current tenant context.
     *
     * @param minorStudentId the unique identifier of the minor student
     * @return the minor student response DTO
     * @throws IllegalArgumentException      if tenant context is not available
     * @throws MinorStudentNotFoundException if no minor student is found with the given identifier
     */
    public GetMinorStudentResponseDTO get(Long minorStudentId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<MinorStudentDataModel> queryResult = minorStudentRepository.findById(
                new MinorStudentDataModel.MinorStudentCompositeId(tenantId, minorStudentId));
        if (queryResult.isPresent()) {
            MinorStudentDataModel found = queryResult.get();
            GetMinorStudentResponseDTO dto = modelMapper.map(found, GetMinorStudentResponseDTO.class);
            modelMapper.map(found.getPersonPII(), dto);
            return dto;
        } else {
            throw new MinorStudentNotFoundException(String.valueOf(minorStudentId));
        }
    }
}
