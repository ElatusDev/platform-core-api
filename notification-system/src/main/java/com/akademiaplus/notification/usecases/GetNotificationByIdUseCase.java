/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a notification by its identifier within the current tenant.
 */
@Service
public class GetNotificationByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final NotificationRepository notificationRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetNotificationByIdUseCase with the required dependencies.
     *
     * @param notificationRepository the repository for notification data access
     * @param tenantContextHolder    the holder for the current tenant context
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetNotificationByIdUseCase(NotificationRepository notificationRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a notification by its identifier within the current tenant context.
     *
     * @param notificationId the unique identifier of the notification
     * @return the notification response DTO
     * @throws IllegalArgumentException    if tenant context is not available
     * @throws EntityNotFoundException     if no notification is found with the given identifier
     */
    public GetNotificationResponseDTO get(Long notificationId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<NotificationDataModel> queryResult = notificationRepository.findById(
                new NotificationDataModel.NotificationCompositeId(tenantId, notificationId));
        if (queryResult.isPresent()) {
            NotificationDataModel found = queryResult.get();
            return modelMapper.map(found, GetNotificationResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.NOTIFICATION, String.valueOf(notificationId));
        }
    }
}
