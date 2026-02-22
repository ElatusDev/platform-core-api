/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all tenant subscriptions in the current tenant.
 *
 * <p>Tenant subscriptions are tenant-scoped, so the Hibernate tenant filter
 * automatically restricts results to the current tenant context.
 */
@Service
public class GetAllTenantSubscriptionsUseCase {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllTenantSubscriptionsUseCase with the required dependencies.
     *
     * @param tenantSubscriptionRepository the repository for tenant subscription data access
     * @param modelMapper                  the mapper for entity-to-DTO conversion
     */
    public GetAllTenantSubscriptionsUseCase(TenantSubscriptionRepository tenantSubscriptionRepository,
                                             ModelMapper modelMapper) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all tenant subscriptions for the current tenant context.
     *
     * @return a list of tenant subscription DTOs
     */
    public List<TenantSubscriptionDTO> getAll() {
        return tenantSubscriptionRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, TenantSubscriptionDTO.class))
                .toList();
    }
}
