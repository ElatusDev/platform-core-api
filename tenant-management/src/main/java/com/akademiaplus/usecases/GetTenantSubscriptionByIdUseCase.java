/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a tenant subscription by its identifier
 * within the current tenant.
 */
@Service
public class GetTenantSubscriptionByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetTenantSubscriptionByIdUseCase with the required dependencies.
     *
     * @param tenantSubscriptionRepository the repository for tenant subscription data access
     * @param tenantContextHolder          the holder for the current tenant context
     * @param modelMapper                  the mapper for entity-to-DTO conversion
     */
    public GetTenantSubscriptionByIdUseCase(TenantSubscriptionRepository tenantSubscriptionRepository,
                                             TenantContextHolder tenantContextHolder,
                                             ModelMapper modelMapper) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a tenant subscription by its identifier within the current tenant context.
     *
     * @param subscriptionId the unique identifier of the subscription
     * @return the tenant subscription response DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no subscription is found with the given identifier
     */
    public TenantSubscriptionDTO get(Long subscriptionId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<TenantSubscriptionDataModel> queryResult = tenantSubscriptionRepository.findById(
                new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(tenantId, subscriptionId));
        if (queryResult.isPresent()) {
            TenantSubscriptionDataModel found = queryResult.get();
            return modelMapper.map(found, TenantSubscriptionDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.TENANT_SUBSCRIPTION, String.valueOf(subscriptionId));
        }
    }
}
