/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles tenant creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Unlike people entities, tenant is not tenant-scoped (it IS the tenant),
 * so there is no PII, auth, or sequential-ID layer involved. The DB
 * generates the {@code tenant_id} via {@code AUTO_INCREMENT}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class TenantCreationUseCase {

    private final TenantRepository tenantRepository;
    private final ApplicationContext applicationContext;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new tenant from the given request.
     *
     * @param dto the tenant creation request
     * @return the persisted tenant mapped to a response DTO
     */
    @Transactional
    public TenantDTO create(TenantCreateRequestDTO dto) {
        TenantDataModel saved = tenantRepository.save(transform(dto));
        return modelMapper.map(saved, TenantDTO.class);
    }

    /**
     * Maps a {@link TenantCreateRequestDTO} to a {@link TenantDataModel}.
     * <p>
     * Uses prototype-scoped bean retrieval to ensure a fresh entity instance,
     * following the same pattern as {@code EmployeeCreationUseCase.transform()}.
     * The {@link ModelMapper} handles type conversions (e.g. {@code URI → String}
     * for {@code websiteUrl}) via converters registered in
     * {@link com.akademiaplus.utilities.config.ModelMapperConfig}.
     *
     * @param dto the tenant creation request
     * @return a detached data model ready for persistence
     */
    public TenantDataModel transform(TenantCreateRequestDTO dto) {
        TenantDataModel model = applicationContext.getBean(TenantDataModel.class);
        modelMapper.map(dto, model);
        return model;
    }
}
