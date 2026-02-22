/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDetailsDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a tenant billing cycle by its identifier
 * within the current tenant.
 */
@Service
public class GetTenantBillingCycleByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final TenantBillingCycleRepository tenantBillingCycleRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetTenantBillingCycleByIdUseCase with the required dependencies.
     *
     * @param tenantBillingCycleRepository the repository for tenant billing cycle data access
     * @param tenantContextHolder          the holder for the current tenant context
     * @param modelMapper                  the mapper for entity-to-DTO conversion
     */
    public GetTenantBillingCycleByIdUseCase(TenantBillingCycleRepository tenantBillingCycleRepository,
                                             TenantContextHolder tenantContextHolder,
                                             ModelMapper modelMapper) {
        this.tenantBillingCycleRepository = tenantBillingCycleRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a tenant billing cycle by its identifier within the current tenant context.
     *
     * @param billingCycleId the unique identifier of the billing cycle
     * @return the billing cycle details DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no billing cycle is found with the given identifier
     */
    public BillingCycleDetailsDTO get(Long billingCycleId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<TenantBillingCycleDataModel> queryResult = tenantBillingCycleRepository.findById(
                new TenantBillingCycleDataModel.TenantBillingCycleCompositeId(tenantId, billingCycleId));
        if (queryResult.isPresent()) {
            TenantBillingCycleDataModel found = queryResult.get();
            return modelMapper.map(found, BillingCycleDetailsDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.TENANT_BILLING_CYCLE, String.valueOf(billingCycleId));
        }
    }
}
