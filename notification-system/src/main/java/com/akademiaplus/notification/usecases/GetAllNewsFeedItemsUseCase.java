/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import openapi.akademiaplus.domain.notification.system.dto.GetNewsFeedItemResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving published news feed items within the current tenant.
 * Supports optional filtering by course ID.
 */
@Service
public class GetAllNewsFeedItemsUseCase {

    private final NewsFeedItemRepository newsFeedItemRepository;
    private final ModelMapper modelMapper;

    public GetAllNewsFeedItemsUseCase(NewsFeedItemRepository newsFeedItemRepository,
                                      ModelMapper modelMapper) {
        this.newsFeedItemRepository = newsFeedItemRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves published news feed items for the current tenant,
     * optionally filtered by course ID.
     *
     * @param courseId optional course filter (null returns all published items)
     * @return list of published news feed item DTOs
     */
    public List<GetNewsFeedItemResponseDTO> getAll(Long courseId) {
        return newsFeedItemRepository.findPublished(courseId).stream()
                .map(entity -> modelMapper.map(entity, GetNewsFeedItemResponseDTO.class))
                .toList();
    }
}
