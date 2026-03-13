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
import openapi.akademiaplus.domain.tenant.management.dto.GetTenantBrandingResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Use case for retrieving tenant branding configuration.
 * Returns the branding for the current tenant, or defaults if none exists.
 */
@Service
public class GetTenantBrandingUseCase {

    private static final String DEFAULT_SCHOOL_NAME = "My School";
    private static final String DEFAULT_PRIMARY_COLOR = "#1976D2";
    private static final String DEFAULT_SECONDARY_COLOR = "#FF9800";

    private final TenantBrandingRepository tenantBrandingRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetTenantBrandingUseCase(TenantBrandingRepository tenantBrandingRepository,
                                    TenantContextHolder tenantContextHolder,
                                    ModelMapper modelMapper) {
        this.tenantBrandingRepository = tenantBrandingRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves the branding configuration for the current tenant.
     * Returns sensible defaults if no custom branding has been configured.
     *
     * @return the tenant branding response DTO
     */
    public GetTenantBrandingResponseDTO get() {
        Long tenantId = tenantContextHolder.requireTenantId();

        return tenantBrandingRepository.findById(tenantId)
                .map(entity -> modelMapper.map(entity, GetTenantBrandingResponseDTO.class))
                .orElseGet(this::buildDefaults);
    }

    private GetTenantBrandingResponseDTO buildDefaults() {
        GetTenantBrandingResponseDTO defaults = new GetTenantBrandingResponseDTO();
        defaults.setSchoolName(DEFAULT_SCHOOL_NAME);
        defaults.setPrimaryColor(DEFAULT_PRIMARY_COLOR);
        defaults.setSecondaryColor(DEFAULT_SECONDARY_COLOR);
        return defaults;
    }
}
