/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import com.akademiaplus.util.base.AbstractPlatformMockDataUseCase;
import com.akademiaplus.util.base.PlatformDataCleanUp;
import com.akademiaplus.util.base.PlatformDataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock notification read status records into the database.
 */
@Service
public class LoadNotificationReadStatusMockDataUseCase
        extends AbstractPlatformMockDataUseCase<NotificationReadStatusDataModel, NotificationReadStatusDataModel> {

    /**
     * Constructs the use case with the notification-read-status-specific data loader and cleanup.
     *
     * @param dataLoader  the platform data loader for notification read statuses
     * @param dataCleanup the platform data cleanup for notification read statuses
     */
    public LoadNotificationReadStatusMockDataUseCase(
            PlatformDataLoader<NotificationReadStatusDataModel, NotificationReadStatusDataModel> dataLoader,
            @Qualifier("notificationReadStatusDataCleanUp")
            PlatformDataCleanUp<NotificationReadStatusDataModel> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
