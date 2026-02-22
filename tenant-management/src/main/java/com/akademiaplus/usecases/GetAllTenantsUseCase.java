/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all tenants.
 *
 * <p>Tenants are not tenant-scoped, so there is no Hibernate filter
 * constraining the result set by tenant ID. All active (non-soft-deleted)
 * tenants are returned.
 */
@Service
public class GetAllTenantsUseCase {

    private final TenantRepository tenantRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllTenantsUseCase with the required dependencies.
     *
     * @param tenantRepository the repository for tenant data access
     * @param modelMapper      the mapper for entity-to-DTO conversion
     */
    public GetAllTenantsUseCase(TenantRepository tenantRepository, ModelMapper modelMapper) {
        this.tenantRepository = tenantRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all active tenants.
     *
     * @return a list of tenant DTOs
     */
    public List<TenantDTO> getAll() {
        return tenantRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, TenantDTO.class))
                .toList();
    }
}
