/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDataModel;

/**
 * Strategy interface for delivering notifications through a specific channel.
 * <p>
 * Each {@link DeliveryChannel} value (WEBAPP, EMAIL, SMS, etc.) can have a
 * corresponding implementation. The {@link NotificationDispatchService}
 * resolves the appropriate strategy at dispatch time.
 *
 * @see WebappDeliveryChannelStrategy
 */
public interface DeliveryChannelStrategy {

    /**
     * Returns the delivery channel this strategy handles.
     *
     * @return the delivery channel
     */
    DeliveryChannel getChannel();

    /**
     * Delivers a notification to the specified recipient.
     *
     * @param notification        the notification to deliver
     * @param recipientIdentifier the recipient identifier (e.g., userId for WEBAPP)
     * @return the result of the delivery attempt
     */
    DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier);
}
