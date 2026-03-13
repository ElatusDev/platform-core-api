/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.newsfeed.NewsFeedStatus;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles creation of news feed items by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching pollution.
 * Status enum is mapped manually since ModelMapper cannot auto-convert String to enum.
 */
@Service
@RequiredArgsConstructor
public class NewsFeedItemCreationUseCase {

    public static final String MAP_NAME = "newsFeedItemMap";

    private final ApplicationContext applicationContext;
    private final NewsFeedItemRepository newsFeedItemRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public NewsFeedItemCreationResponseDTO create(NewsFeedItemCreationRequestDTO dto) {
        NewsFeedItemDataModel saved = newsFeedItemRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class);
    }

    /**
     * Transforms the request DTO into a persistence data model.
     * <p>
     * {@code status} is converted manually because ModelMapper cannot
     * auto-convert String to Java enum. Defaults to DRAFT if not provided.
     * When status is PUBLISHED, {@code publishedAt} is set automatically.
     */
    public NewsFeedItemDataModel transform(NewsFeedItemCreationRequestDTO dto) {
        final NewsFeedItemDataModel model = applicationContext.getBean(NewsFeedItemDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        if (dto.getStatus() != null) {
            NewsFeedStatus status = NewsFeedStatus.valueOf(dto.getStatus().name());
            model.setStatus(status);
            if (status == NewsFeedStatus.PUBLISHED) {
                model.setPublishedAt(LocalDateTime.now());
            }
        } else {
            model.setStatus(NewsFeedStatus.DRAFT);
        }

        return model;
    }
}
