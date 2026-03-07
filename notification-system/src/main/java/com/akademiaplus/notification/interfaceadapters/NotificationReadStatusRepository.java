/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for {@link NotificationReadStatusDataModel}.
 * <p>
 * Platform-level repository — no tenant filtering is applied.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface NotificationReadStatusRepository extends JpaRepository<NotificationReadStatusDataModel, Long> {

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    @Query("SELECT rs.notificationId FROM NotificationReadStatusDataModel rs WHERE rs.userId = :userId")
    Set<Long> findReadNotificationIdsByUserId(@Param("userId") Long userId);
}
