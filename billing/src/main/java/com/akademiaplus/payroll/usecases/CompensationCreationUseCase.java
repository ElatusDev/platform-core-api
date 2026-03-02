/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles compensation creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the {@code collaborators} M2M collection.
 */
@Service
@RequiredArgsConstructor
public class CompensationCreationUseCase {
    public static final String MAP_NAME = "compensationMap";

    private final ApplicationContext applicationContext;
    private final CompensationRepository compensationRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public CompensationCreationResponseDTO create(CompensationCreationRequestDTO dto) {
        CompensationDataModel saved = compensationRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, CompensationCreationResponseDTO.class);
    }

    public CompensationDataModel transform(CompensationCreationRequestDTO dto) {
        final CompensationDataModel model = applicationContext.getBean(CompensationDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }
}
