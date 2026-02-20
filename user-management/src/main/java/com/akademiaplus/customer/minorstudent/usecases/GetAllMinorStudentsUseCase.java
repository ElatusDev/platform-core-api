/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all minor students in the current tenant.
 */
@Service
public class GetAllMinorStudentsUseCase {

    private final MinorStudentRepository minorStudentRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllMinorStudentsUseCase with the required dependencies.
     *
     * @param minorStudentRepository the repository for minor student data access
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetAllMinorStudentsUseCase(MinorStudentRepository minorStudentRepository,
                                      ModelMapper modelMapper) {
        this.minorStudentRepository = minorStudentRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all minor students for the current tenant context.
     *
     * @return a list of minor student response DTOs
     */
    public List<GetMinorStudentResponseDTO> getAll() {
        return minorStudentRepository.findAll().stream()
                .map(dataModel -> {
                    GetMinorStudentResponseDTO dto = modelMapper.map(dataModel, GetMinorStudentResponseDTO.class);
                    modelMapper.map(dataModel.getPersonPII(), dto);
                    return dto;
                })
                .toList();
    }
}
