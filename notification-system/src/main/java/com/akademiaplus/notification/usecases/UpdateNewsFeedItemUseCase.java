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
import com.akademiaplus.newsfeed.NewsFeedStatus;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles updating an existing news feed item by mapping new field values
 * onto the persisted entity.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) that skips the entity ID field
 * to prevent overwriting the composite key during mapping.
 * When status transitions to PUBLISHED, publishedAt is set automatically.
 */
@Service
@RequiredArgsConstructor
public class UpdateNewsFeedItemUseCase {

    public static final String MAP_NAME = "newsFeedItemUpdateMap";

    private final NewsFeedItemRepository newsFeedItemRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates the news feed item identified by {@code newsFeedItemId}
     * within the current tenant context.
     *
     * @param newsFeedItemId the entity-specific news feed item ID
     * @param dto            the updated field values
     * @return response containing the news feed item ID
     */
    @Transactional
    public NewsFeedItemCreationResponseDTO update(Long newsFeedItemId,
                                                   NewsFeedItemCreationRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        NewsFeedItemDataModel existing = newsFeedItemRepository
                .findById(new NewsFeedItemDataModel.NewsFeedItemCompositeId(tenantId, newsFeedItemId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.NEWS_FEED_ITEM, String.valueOf(newsFeedItemId)));

        modelMapper.map(dto, existing, MAP_NAME);

        if (dto.getCourseId() != null && dto.getCourseId().isPresent()) {
            existing.setCourseId(dto.getCourseId().get());
        }
        if (dto.getImageUrl() != null && dto.getImageUrl().isPresent()) {
            existing.setImageUrl(dto.getImageUrl().get());
        }

        if (dto.getStatus() != null) {
            NewsFeedStatus newStatus = NewsFeedStatus.valueOf(dto.getStatus().name());
            existing.setStatus(newStatus);
            if (newStatus == NewsFeedStatus.PUBLISHED && existing.getPublishedAt() == null) {
                existing.setPublishedAt(LocalDateTime.now());
            }
        }

        NewsFeedItemDataModel saved = newsFeedItemRepository.saveAndFlush(existing);
        return modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class);
    }
}
