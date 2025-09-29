/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

/**
 * Enumeration of notification priority levels.
 * Determines delivery urgency, retry behavior, and user interface treatment.
 * Higher priority notifications may bypass certain user preferences.
 */
public enum NotificationPriority {
    /**
     * Low priority notifications for non-urgent information.
     * May be batched or delayed based on user preferences.
     */
    LOW,

    /**
     * Normal priority for standard operational notifications.
     * Default priority level for most notification types.
     */
    NORMAL,

    /**
     * High priority for important but not critical notifications.
     * Delivered promptly but respects user do-not-disturb settings.
     */
    HIGH,

    /**
     * Urgent priority for critical notifications requiring immediate attention.
     * May bypass user preferences and retry more aggressively.
     */
    URGENT
}