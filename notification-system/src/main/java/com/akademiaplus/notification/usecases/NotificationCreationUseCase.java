/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationPriority;
import com.akademiaplus.notifications.NotificationType;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles notification creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class NotificationCreationUseCase {
    public static final String MAP_NAME = "notificationMap";

    private final ApplicationContext applicationContext;
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public NotificationCreationResponseDTO create(NotificationCreationRequestDTO dto) {
        NotificationDataModel saved = notificationRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, NotificationCreationResponseDTO.class);
    }

    /**
     * Transforms the request DTO into a persistence data model.
     * <p>
     * {@code type} and {@code priority} are converted manually because
     * ModelMapper cannot auto-convert {@link String} to Java enum types.
     * These fields are skipped in the named TypeMap and set here via
     * {@link NotificationType#valueOf} / {@link NotificationPriority#valueOf}.
     *
     * @param dto the creation request DTO
     * @return a populated data model ready for persistence
     */
    public NotificationDataModel transform(NotificationCreationRequestDTO dto) {
        final NotificationDataModel model = applicationContext.getBean(NotificationDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        if (dto.getType() != null) {
            model.setType(NotificationType.valueOf(dto.getType()));
        }
        if (dto.getPriority() != null) {
            model.setPriority(NotificationPriority.valueOf(dto.getPriority()));
        }

        return model;
    }
}
