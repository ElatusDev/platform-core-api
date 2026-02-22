/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all tenant billing cycles in the current tenant.
 *
 * <p>Tenant billing cycles are tenant-scoped, so the Hibernate tenant filter
 * automatically restricts results to the current tenant context.
 */
@Service
public class GetAllTenantBillingCyclesUseCase {

    private final TenantBillingCycleRepository tenantBillingCycleRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllTenantBillingCyclesUseCase with the required dependencies.
     *
     * @param tenantBillingCycleRepository the repository for tenant billing cycle data access
     * @param modelMapper                  the mapper for entity-to-DTO conversion
     */
    public GetAllTenantBillingCyclesUseCase(TenantBillingCycleRepository tenantBillingCycleRepository,
                                             ModelMapper modelMapper) {
        this.tenantBillingCycleRepository = tenantBillingCycleRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all tenant billing cycles for the current tenant context.
     *
     * @return a list of billing cycle DTOs
     */
    public List<BillingCycleDTO> getAll() {
        return tenantBillingCycleRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, BillingCycleDTO.class))
                .toList();
    }
}
