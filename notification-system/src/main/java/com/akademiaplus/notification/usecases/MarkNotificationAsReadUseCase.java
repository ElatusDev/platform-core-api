/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationReadStatusRepository;
import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Use case for marking a notification as read for a specific user.
 * <p>
 * Idempotent: if the notification is already marked as read, no action is taken.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class MarkNotificationAsReadUseCase {

    private final NotificationReadStatusRepository notificationReadStatusRepository;

    public MarkNotificationAsReadUseCase(NotificationReadStatusRepository notificationReadStatusRepository) {
        this.notificationReadStatusRepository = notificationReadStatusRepository;
    }

    public void markAsRead(Long notificationId, Long userId) {
        if (notificationReadStatusRepository.existsByNotificationIdAndUserId(notificationId, userId)) {
            return;
        }
        NotificationReadStatusDataModel readStatus = new NotificationReadStatusDataModel();
        readStatus.setNotificationId(notificationId);
        readStatus.setUserId(userId);
        readStatus.setReadAt(LocalDateTime.now());
        notificationReadStatusRepository.save(readStatus);
    }
}
