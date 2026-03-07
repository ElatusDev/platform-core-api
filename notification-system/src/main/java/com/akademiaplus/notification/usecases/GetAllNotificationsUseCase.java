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
     * Retrieves notifications for the current tenant context.
     *
     * <p>When {@code targetUserId} is provided, only notifications targeted
     * to that user are returned.</p>
     *
     * @param targetUserId optional target user ID filter
     * @return a list of notification response DTOs
     */
    public List<GetNotificationResponseDTO> getAll(Long targetUserId) {
        var source = (targetUserId != null)
                ? notificationRepository.findByTargetUserId(targetUserId)
                : notificationRepository.findAll();
        return source.stream()
                .map(dataModel -> modelMapper.map(dataModel, GetNotificationResponseDTO.class))
                .toList();
    }
}
