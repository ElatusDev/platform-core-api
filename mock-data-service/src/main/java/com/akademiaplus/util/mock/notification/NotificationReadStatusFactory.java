/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link NotificationReadStatusDataModel} instances with fake data.
 *
 * <p>Requires notification IDs and user IDs to be injected via setters
 * before {@link #generate(int)} is called.</p>
 */
@Component
public class NotificationReadStatusFactory implements DataFactory<NotificationReadStatusDataModel> {

    private final ApplicationContext applicationContext;

    @Setter
    private List<Long> availableNotificationIds = List.of();

    @Setter
    private List<Long> availableUserIds = List.of();

    /**
     * Constructs the factory with Spring's application context for prototype bean creation.
     *
     * @param applicationContext the Spring application context
     */
    public NotificationReadStatusFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<NotificationReadStatusDataModel> generate(int count) {
        if (availableNotificationIds.isEmpty()) {
            throw new IllegalStateException("availableNotificationIds must be set before generating notification read statuses");
        }
        if (availableUserIds.isEmpty()) {
            throw new IllegalStateException("availableUserIds must be set before generating notification read statuses");
        }

        List<NotificationReadStatusDataModel> statuses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long notificationId = availableNotificationIds.get(i % availableNotificationIds.size());
            Long userId = availableUserIds.get(i % availableUserIds.size());

            NotificationReadStatusDataModel model = applicationContext.getBean(NotificationReadStatusDataModel.class);
            model.setNotificationId(notificationId);
            model.setUserId(userId);
            model.setReadAt(LocalDateTime.now().minusMinutes(i + 1L));
            statuses.add(model);
        }
        return statuses;
    }
}
