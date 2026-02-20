/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all notifications in the current tenant.
 */
@Service
public class GetAllNotificationsUseCase {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllNotificationsUseCase with the required dependencies.
     *
     * @param notificationRepository the repository for notification data access
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetAllNotificationsUseCase(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all notifications for the current tenant context.
     *
     * @return a list of notification response DTOs
     */
    public List<GetNotificationResponseDTO> getAll() {
        return notificationRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetNotificationResponseDTO.class))
                .toList();
    }
}
