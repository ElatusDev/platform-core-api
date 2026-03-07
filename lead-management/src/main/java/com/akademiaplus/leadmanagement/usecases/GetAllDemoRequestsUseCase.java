/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieves all demo requests.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class GetAllDemoRequestsUseCase {

    private final DemoRequestRepository demoRequestRepository;
    private final ModelMapper modelMapper;

    /**
     * Returns all demo requests mapped to response DTOs.
     *
     * @return list of demo request responses
     */
    public List<GetDemoRequestResponseDTO> getAll() {
        return demoRequestRepository.findAll().stream()
                .map(model -> modelMapper.map(model, GetDemoRequestResponseDTO.class))
                .toList();
    }
}
