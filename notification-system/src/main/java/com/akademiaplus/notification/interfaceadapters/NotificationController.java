/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.DeleteNotificationUseCase;
import com.akademiaplus.notification.usecases.GetAllNotificationsUseCase;
import com.akademiaplus.notification.usecases.GetNotificationByIdUseCase;
import com.akademiaplus.notification.usecases.NotificationCreationUseCase;
import com.akademiaplus.notification.usecases.NotificationDispatchService;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import openapi.akademiaplus.domain.notification.system.api.NotificationsApi;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationDispatchResponseDTO;
import org.modelmapper.ModelMapper;
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
    private final DeleteNotificationUseCase deleteNotificationUseCase;
    private final NotificationDispatchService notificationDispatchService;
    private final ModelMapper modelMapper;

    public NotificationController(NotificationCreationUseCase notificationCreationUseCase,
                                  GetAllNotificationsUseCase getAllNotificationsUseCase,
                                  GetNotificationByIdUseCase getNotificationByIdUseCase,
                                  DeleteNotificationUseCase deleteNotificationUseCase,
                                  NotificationDispatchService notificationDispatchService,
                                  ModelMapper modelMapper) {
        this.notificationCreationUseCase = notificationCreationUseCase;
        this.getAllNotificationsUseCase = getAllNotificationsUseCase;
        this.getNotificationByIdUseCase = getNotificationByIdUseCase;
        this.deleteNotificationUseCase = deleteNotificationUseCase;
        this.notificationDispatchService = notificationDispatchService;
        this.modelMapper = modelMapper;
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

    @Override
    public ResponseEntity<Void> deleteNotification(Long notificationId) {
        deleteNotificationUseCase.delete(notificationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NotificationDispatchResponseDTO> dispatchNotification(Long notificationId) {
        NotificationDataModel notification = getNotificationByIdUseCase.getEntity(notificationId);
        NotificationDeliveryDataModel delivery = notificationDispatchService.dispatch(notification);
        NotificationDispatchResponseDTO response = modelMapper.map(delivery, NotificationDispatchResponseDTO.class);
        return ResponseEntity.ok(response);
    }
}
