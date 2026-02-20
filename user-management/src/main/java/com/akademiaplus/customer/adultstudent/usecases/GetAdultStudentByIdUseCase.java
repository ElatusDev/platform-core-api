/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetAdultStudentByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final AdultStudentRepository adultStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetAdultStudentByIdUseCase(AdultStudentRepository adultStudentRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.adultStudentRepository = adultStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    public GetAdultStudentResponseDTO get(Long adultStudentId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<AdultStudentDataModel> result = adultStudentRepository.findById(
                new AdultStudentDataModel.AdultStudentCompositeId(tenantId, adultStudentId));
        if(result.isPresent()) {
            AdultStudentDataModel found = result.get();
            GetAdultStudentResponseDTO dto = modelMapper.map(found, GetAdultStudentResponseDTO.class);
            modelMapper.map(found.getPersonPII(), dto);
            return dto;
        } else {
            throw new EntityNotFoundException(EntityType.ADULT_STUDENT, String.valueOf(adultStudentId));
        }
    }
}
