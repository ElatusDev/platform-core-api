/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends TenantScopedRepository<NotificationDataModel, NotificationDataModel.NotificationCompositeId> {

    /**
     * Finds notifications scheduled for delivery at or before the given time.
     * <p>
     * This query runs cross-tenant when the Hibernate tenant filter is inactive
     * (e.g., in scheduler context). Returns only non-deleted notifications with
     * a non-null {@code scheduledAt} timestamp.
     *
     * @param now the cutoff time
     * @return notifications due for dispatch
     */
    @Query("SELECT n FROM NotificationDataModel n WHERE n.scheduledAt <= :now "
         + "AND n.scheduledAt IS NOT NULL AND n.deletedAt IS NULL")
    List<NotificationDataModel> findScheduledBefore(@Param("now") LocalDateTime now);

    /**
     * Finds notifications targeted to a specific user within the current tenant.
     *
     * @param targetUserId the target user ID to filter by
     * @return matching notifications
     */
    List<NotificationDataModel> findByTargetUserId(Long targetUserId);
}
