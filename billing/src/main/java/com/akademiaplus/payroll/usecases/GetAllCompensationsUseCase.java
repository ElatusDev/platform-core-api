/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all compensations in the current tenant.
 */
@Service
public class GetAllCompensationsUseCase {

    private final CompensationRepository compensationRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllCompensationsUseCase with the required dependencies.
     *
     * @param compensationRepository the repository for compensation data access
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetAllCompensationsUseCase(CompensationRepository compensationRepository, ModelMapper modelMapper) {
        this.compensationRepository = compensationRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all compensations for the current tenant context.
     *
     * @return a list of compensation response DTOs
     */
    public List<GetCompensationResponseDTO> getAll() {
        return compensationRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetCompensationResponseDTO.class))
                .toList();
    }
}
