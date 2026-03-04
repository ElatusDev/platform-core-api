/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activates Spring's task scheduling infrastructure for the notification module.
 * <p>
 * Enables {@link org.springframework.scheduling.annotation.Scheduled @Scheduled}
 * methods, used by {@link com.akademiaplus.notification.usecases.ScheduledNotificationDispatcher}
 * to periodically dispatch due notifications.
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfiguration {
}
