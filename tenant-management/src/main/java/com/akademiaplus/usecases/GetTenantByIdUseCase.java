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
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDetailsDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a tenant by its identifier.
 *
 * <p>Unlike tenant-scoped entities, tenants use a simple {@code Long} primary
 * key — there is no composite key and no {@code TenantContextHolder} lookup.
 */
@Service
public class GetTenantByIdUseCase {

    private final TenantRepository tenantRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetTenantByIdUseCase with the required dependencies.
     *
     * @param tenantRepository the repository for tenant data access
     * @param modelMapper      the mapper for entity-to-DTO conversion
     */
    public GetTenantByIdUseCase(TenantRepository tenantRepository,
                                 ModelMapper modelMapper) {
        this.tenantRepository = tenantRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a tenant by its identifier.
     *
     * @param tenantId the unique identifier of the tenant
     * @return the tenant details DTO
     * @throws EntityNotFoundException if no tenant is found with the given identifier
     */
    public TenantDetailsDTO get(Long tenantId) {
        Optional<TenantDataModel> queryResult = tenantRepository.findById(tenantId);
        if (queryResult.isPresent()) {
            TenantDataModel found = queryResult.get();
            return modelMapper.map(found, TenantDetailsDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.TENANT, String.valueOf(tenantId));
        }
    }
}
