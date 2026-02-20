/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.notification.NotificationDeliveryFactory.NotificationDeliveryRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock notification delivery records into the database.
 */
@Service
public class LoadNotificationDeliveryMockDataUseCase
        extends AbstractMockDataUseCase<NotificationDeliveryRequest, NotificationDeliveryDataModel, Long> {

    public LoadNotificationDeliveryMockDataUseCase(
            DataLoader<NotificationDeliveryRequest, NotificationDeliveryDataModel, Long> dataLoader,
            @Qualifier("notificationDeliveryDataCleanUp")
            DataCleanUp<NotificationDeliveryDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
