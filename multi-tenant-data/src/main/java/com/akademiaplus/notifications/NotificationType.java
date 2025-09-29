/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

/**
 * Enumeration of notification types for categorization and processing.
 * Used to classify notifications by their business purpose and
 * apply appropriate delivery rules and user preferences.
 */
public enum NotificationType {
    /**
     * Reminder notifications for upcoming courses or classes.
     * Typically sent before scheduled educational events.
     */
    COURSE_REMINDER,

    /**
     * Notifications about payment due dates or overdue amounts.
     * Used for billing and payment management communications.
     */
    PAYMENT_DUE,

    /**
     * Confirmation notifications for successful course enrollments.
     * Sent when users register for new courses or programs.
     */
    ENROLLMENT_CONFIRMATION,

    /**
     * Notifications about changes to course schedules or locations.
     * Alerts users to modifications in their educational calendar.
     */
    SCHEDULE_CHANGE,

    /**
     * System maintenance and downtime notifications.
     * Informs users about planned or emergency system updates.
     */
    SYSTEM_MAINTENANCE,

    /**
     * Marketing and promotional notifications.
     * Used for new course announcements and special offers.
     */
    PROMOTIONAL,

    /**
     * General announcements from the educational institution.
     * Administrative communications and important updates.
     */
    ANNOUNCEMENT,

    /**
     * Assignment and homework reminder notifications.
     * Helps students stay on track with their coursework.
     */
    ASSIGNMENT_REMINDER,

    /**
     * Grade and assessment result notifications.
     * Informs students when new grades are available.
     */
    GRADE_NOTIFICATION
}