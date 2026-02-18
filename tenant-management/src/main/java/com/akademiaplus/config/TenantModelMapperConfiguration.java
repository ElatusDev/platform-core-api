/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.usecases.TenantCreationUseCase;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for tenant DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching {@code TenantCreateRequestDTO.taxId}
 * (String) into {@code TenantDataModel.tenantId} (Long) via the shared "Id"
 * token. Without this skip rule, the {@code NumberConverter} attempts to parse
 * the tax identifier as a numeric tenant ID, causing a {@code MappingException}.
 *
 * @see TenantCreationUseCase
 */
@Configuration
public class TenantModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public TenantModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        modelMapper.createTypeMap(
                TenantCreateRequestDTO.class,
                TenantDataModel.class,
                TenantCreationUseCase.MAP_NAME
        ).addMappings(mapper ->
                mapper.skip(TenantDataModel::setTenantId)
        ).implicitMappings();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }
}
