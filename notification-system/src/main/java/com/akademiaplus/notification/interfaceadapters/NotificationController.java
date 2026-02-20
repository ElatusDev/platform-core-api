/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.GetAllNotificationsUseCase;
import com.akademiaplus.notification.usecases.GetNotificationByIdUseCase;
import com.akademiaplus.notification.usecases.NotificationCreationUseCase;
import openapi.akademiaplus.domain.notification.system.api.NotificationsApi;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for notification management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/notification-system")
public class NotificationController implements NotificationsApi {

    private final NotificationCreationUseCase notificationCreationUseCase;
    private final GetAllNotificationsUseCase getAllNotificationsUseCase;
    private final GetNotificationByIdUseCase getNotificationByIdUseCase;

    public NotificationController(NotificationCreationUseCase notificationCreationUseCase,
                                  GetAllNotificationsUseCase getAllNotificationsUseCase,
                                  GetNotificationByIdUseCase getNotificationByIdUseCase) {
        this.notificationCreationUseCase = notificationCreationUseCase;
        this.getAllNotificationsUseCase = getAllNotificationsUseCase;
        this.getNotificationByIdUseCase = getNotificationByIdUseCase;
    }

    @Override
    public ResponseEntity<NotificationCreationResponseDTO> createNotification(
            NotificationCreationRequestDTO notificationCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationCreationUseCase.create(notificationCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetNotificationResponseDTO>> getNotifications() {
        return ResponseEntity.ok(getAllNotificationsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetNotificationResponseDTO> getNotificationById(Long notificationId) {
        return ResponseEntity.ok(getNotificationByIdUseCase.get(notificationId));
    }
}
