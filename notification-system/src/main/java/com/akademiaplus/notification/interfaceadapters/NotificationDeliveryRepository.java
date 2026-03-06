/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link NotificationDeliveryDataModel} entities.
 */
@Repository
public interface NotificationDeliveryRepository extends TenantScopedRepository<NotificationDeliveryDataModel, NotificationDeliveryDataModel.NotificationDeliveryCompositeId> {

    /**
     * Finds all delivery records for a given notification.
     *
     * @param notificationId the notification identifier
     * @return list of delivery records
     */
    List<NotificationDeliveryDataModel> findByNotificationId(Long notificationId);

    /**
     * Finds all delivery records for a given notification and channel.
     *
     * @param notificationId the notification identifier
     * @param channel        the delivery channel
     * @return list of delivery records
     */
    List<NotificationDeliveryDataModel> findByNotificationIdAndChannel(Long notificationId, DeliveryChannel channel);

    /**
     * Finds a delivery record by its delivery ID.
     *
     * @param notificationDeliveryId the delivery identifier
     * @return optional delivery record
     */
    Optional<NotificationDeliveryDataModel> findByNotificationDeliveryId(Long notificationDeliveryId);
}
