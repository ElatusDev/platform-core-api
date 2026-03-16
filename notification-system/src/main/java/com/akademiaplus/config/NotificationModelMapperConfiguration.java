/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.notification.usecases.EmailTemplateCreationUseCase;
import com.akademiaplus.notification.usecases.NewsFeedItemCreationUseCase;
import com.akademiaplus.notification.usecases.NotificationCreationUseCase;
import com.akademiaplus.notification.usecases.UpdateNewsFeedItemUseCase;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailTemplateRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for notification DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into the entity ID field.
 */
@Configuration
public class NotificationModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public NotificationModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerNotificationMap();
        registerEmailTemplateCreateMap();
        registerNewsFeedItemCreateMap();
        registerNewsFeedItemUpdateMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerNotificationMap() {
        modelMapper.createTypeMap(
                NotificationCreationRequestDTO.class,
                NotificationDataModel.class,
                NotificationCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(NotificationDataModel::setNotificationId);
            mapper.skip(NotificationDataModel::setType);
            mapper.skip(NotificationDataModel::setPriority);
        }).implicitMappings();
    }

    private void registerEmailTemplateCreateMap() {
        modelMapper.createTypeMap(
                CreateEmailTemplateRequestDTO.class,
                EmailTemplateDataModel.class,
                EmailTemplateCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(EmailTemplateDataModel::setTemplateId);
            mapper.skip(EmailTemplateDataModel::setVariables);
        }).implicitMappings();
    }

    private void registerNewsFeedItemCreateMap() {
        modelMapper.createTypeMap(
                NewsFeedItemCreationRequestDTO.class,
                NewsFeedItemDataModel.class,
                NewsFeedItemCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(NewsFeedItemDataModel::setNewsFeedItemId);
            mapper.skip(NewsFeedItemDataModel::setStatus);
            mapper.skip(NewsFeedItemDataModel::setCourseId);
            mapper.skip(NewsFeedItemDataModel::setImageUrl);
        }).implicitMappings();
    }

    private void registerNewsFeedItemUpdateMap() {
        modelMapper.createTypeMap(
                NewsFeedItemCreationRequestDTO.class,
                NewsFeedItemDataModel.class,
                UpdateNewsFeedItemUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(NewsFeedItemDataModel::setNewsFeedItemId);
            mapper.skip(NewsFeedItemDataModel::setStatus);
            mapper.skip(NewsFeedItemDataModel::setCourseId);
            mapper.skip(NewsFeedItemDataModel::setImageUrl);
        }).implicitMappings();
    }
}
