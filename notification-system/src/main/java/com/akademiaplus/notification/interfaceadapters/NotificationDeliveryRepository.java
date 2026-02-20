/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link NotificationDeliveryDataModel} entities.
 */
@Repository
public interface NotificationDeliveryRepository extends TenantScopedRepository<NotificationDeliveryDataModel, NotificationDeliveryDataModel.NotificationDeliveryCompositeId> {
}
