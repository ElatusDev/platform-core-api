/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.TutorDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a tutor by their identifier within the current tenant.
 */
@Service
public class GetTutorByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final TutorRepository tutorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetTutorByIdUseCase with the required dependencies.
     *
     * @param tutorRepository     the repository for tutor data access
     * @param tenantContextHolder the holder for the current tenant context
     * @param modelMapper         the mapper for entity-to-DTO conversion
     */
    public GetTutorByIdUseCase(TutorRepository tutorRepository,
                               TenantContextHolder tenantContextHolder,
                               ModelMapper modelMapper) {
        this.tutorRepository = tutorRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a tutor by their identifier within the current tenant context.
     *
     * @param tutorId the unique identifier of the tutor
     * @return the tutor response DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException   if no tutor is found with the given identifier
     */
    public GetTutorResponseDTO get(Long tutorId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<TutorDataModel> queryResult = tutorRepository.findById(
                new TutorDataModel.TutorCompositeId(tenantId, tutorId));
        if (queryResult.isPresent()) {
            TutorDataModel found = queryResult.get();
            GetTutorResponseDTO dto = modelMapper.map(found, GetTutorResponseDTO.class);
            modelMapper.map(found.getPersonPII(), dto);
            return dto;
        } else {
            throw new EntityNotFoundException(EntityType.TUTOR, String.valueOf(tutorId));
        }
    }
}
