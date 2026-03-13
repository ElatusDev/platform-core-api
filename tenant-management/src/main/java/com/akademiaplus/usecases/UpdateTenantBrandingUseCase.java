/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantBrandingRepository;
import com.akademiaplus.tenancy.TenantBrandingDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.GetTenantBrandingResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantBrandingUpdateRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles creating or updating the tenant branding configuration (upsert).
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) that skips the tenantId field
 * to prevent overwriting the primary key during mapping.
 * If no branding exists for the current tenant, a new entity is created.
 */
@Service
@RequiredArgsConstructor
public class UpdateTenantBrandingUseCase {

    public static final String MAP_NAME = "tenantBrandingUpdateMap";

    private final ApplicationContext applicationContext;
    private final TenantBrandingRepository tenantBrandingRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Creates or updates the branding configuration for the current tenant.
     *
     * @param dto the branding update request
     * @return the updated branding response DTO
     */
    @Transactional
    public GetTenantBrandingResponseDTO upsert(TenantBrandingUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        TenantBrandingDataModel entity = tenantBrandingRepository.findById(tenantId)
                .orElseGet(() -> {
                    TenantBrandingDataModel newEntity = applicationContext.getBean(TenantBrandingDataModel.class);
                    newEntity.setTenantId(tenantId);
                    return newEntity;
                });

        modelMapper.map(dto, entity, MAP_NAME);

        TenantBrandingDataModel saved = tenantBrandingRepository.saveAndFlush(entity);
        return modelMapper.map(saved, GetTenantBrandingResponseDTO.class);
    }
}
