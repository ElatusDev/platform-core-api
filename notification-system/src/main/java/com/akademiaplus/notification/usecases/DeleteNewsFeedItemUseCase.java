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
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link NewsFeedItemDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 */
@Service
public class DeleteNewsFeedItemUseCase {

    private final NewsFeedItemRepository repository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteNewsFeedItemUseCase(NewsFeedItemRepository repository,
                                     TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link NewsFeedItemDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param newsFeedItemId the entity-specific ID
     */
    @Transactional
    public void delete(Long newsFeedItemId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new NewsFeedItemDataModel.NewsFeedItemCompositeId(tenantId, newsFeedItemId),
                EntityType.NEWS_FEED_ITEM,
                String.valueOf(newsFeedItemId));
    }
}
