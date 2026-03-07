/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Retrieves a single demo request by its ID.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class GetDemoRequestByIdUseCase {

    /** Error constant for demo request not found. */
    public static final String ERROR_DEMO_REQUEST_NOT_FOUND =
            "Demo request not found with id: ";

    private final DemoRequestRepository demoRequestRepository;
    private final ModelMapper modelMapper;

    /**
     * Finds a demo request by ID.
     *
     * @param id the demo request ID
     * @return the demo request mapped to a response DTO
     * @throws EntityNotFoundException if the demo request does not exist
     */
    public GetDemoRequestResponseDTO get(Long id) {
        DemoRequestDataModel model = demoRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.DEMO_REQUEST, id.toString()));
        return modelMapper.map(model, GetDemoRequestResponseDTO.class);
    }
}
