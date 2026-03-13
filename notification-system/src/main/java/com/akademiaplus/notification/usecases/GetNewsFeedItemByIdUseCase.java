/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.GetNewsFeedItemResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Use case for retrieving a news feed item by its identifier within the current tenant.
 */
@Service
public class GetNewsFeedItemByIdUseCase {

    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final NewsFeedItemRepository newsFeedItemRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetNewsFeedItemByIdUseCase(NewsFeedItemRepository newsFeedItemRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.newsFeedItemRepository = newsFeedItemRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves the news feed item entity by its identifier within the current tenant context.
     *
     * @param newsFeedItemId the unique identifier of the news feed item
     * @return the news feed item data model
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no news feed item is found with the given identifier
     */
    public NewsFeedItemDataModel getEntity(Long newsFeedItemId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        return newsFeedItemRepository.findById(
                new NewsFeedItemDataModel.NewsFeedItemCompositeId(tenantId, newsFeedItemId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.NEWS_FEED_ITEM, String.valueOf(newsFeedItemId)));
    }

    /**
     * Retrieves a news feed item by its identifier within the current tenant context.
     *
     * @param newsFeedItemId the unique identifier of the news feed item
     * @return the news feed item response DTO
     */
    public GetNewsFeedItemResponseDTO get(Long newsFeedItemId) {
        NewsFeedItemDataModel found = getEntity(newsFeedItemId);
        return modelMapper.map(found, GetNewsFeedItemResponseDTO.class);
    }
}
