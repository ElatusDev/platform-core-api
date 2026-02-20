/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all tutors in the current tenant.
 */
@Service
public class GetAllTutorsUseCase {

    private final TutorRepository tutorRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllTutorsUseCase with the required dependencies.
     *
     * @param tutorRepository the repository for tutor data access
     * @param modelMapper     the mapper for entity-to-DTO conversion
     */
    public GetAllTutorsUseCase(TutorRepository tutorRepository, ModelMapper modelMapper) {
        this.tutorRepository = tutorRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all tutors for the current tenant context.
     *
     * @return a list of tutor response DTOs
     */
    public List<GetTutorResponseDTO> getAll() {
        return tutorRepository.findAll().stream()
                .map(dataModel -> {
                    GetTutorResponseDTO dto = modelMapper.map(dataModel, GetTutorResponseDTO.class);
                    modelMapper.map(dataModel.getPersonPII(), dto);
                    return dto;
                })
                .toList();
    }
}
