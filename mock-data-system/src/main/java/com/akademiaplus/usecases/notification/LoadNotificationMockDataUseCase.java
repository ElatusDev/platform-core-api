/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock notification records into the database.
 */
@Service
public class LoadNotificationMockDataUseCase
        extends AbstractMockDataUseCase<NotificationCreationRequestDTO, NotificationDataModel, NotificationDataModel.NotificationCompositeId> {

    public LoadNotificationMockDataUseCase(
            DataLoader<NotificationCreationRequestDTO, NotificationDataModel, NotificationDataModel.NotificationCompositeId> dataLoader,
            @Qualifier("notificationDataCleanUp")
            DataCleanUp<NotificationDataModel, NotificationDataModel.NotificationCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
