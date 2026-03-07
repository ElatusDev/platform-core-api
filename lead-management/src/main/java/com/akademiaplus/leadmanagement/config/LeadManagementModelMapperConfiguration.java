/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.config;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.usecases.DemoRequestCreationUseCase;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for lead-management DTO → DataModel conversions.
 *
 * @see DemoRequestCreationUseCase
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class LeadManagementModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public LeadManagementModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerDemoRequestMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerDemoRequestMap() {
        modelMapper.createTypeMap(
                DemoRequestCreationRequestDTO.class,
                DemoRequestDataModel.class,
                DemoRequestCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(DemoRequestDataModel::setDemoRequestId);
            mapper.skip(DemoRequestDataModel::setStatus);
        }).implicitMappings();
    }
}
