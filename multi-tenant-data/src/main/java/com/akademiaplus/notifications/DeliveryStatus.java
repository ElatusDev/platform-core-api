/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

/**
 * Enumeration of delivery status states for tracking notification progress.
 * Represents the current state of a notification delivery attempt
 * from initial creation through final delivery or failure.
 */
public enum DeliveryStatus {
    /**
     * Notification is queued for delivery but not yet sent.
     * Initial state for scheduled or queued notifications.
     */
    PENDING,

    /**
     * Notification has been sent to the delivery service provider.
     * Indicates the platform has initiated the delivery attempt.
     */
    SENT,

    /**
     * Delivery confirmation received from the service provider.
     * Indicates successful delivery to the recipient's device or inbox.
     */
    DELIVERED,

    /**
     * Recipient has acknowledged or interacted with the notification.
     * Highest level of delivery confirmation indicating user engagement.
     */
    ACKNOWLEDGED,

    /**
     * Delivery attempt failed due to an error or issue.
     * Requires review of failure reason and potential retry.
     */
    FAILED,

    /**
     * Notification expired before delivery could be completed.
     * Occurs when delivery attempts exceed the expiration time.
     */
    EXPIRED,

    /**
     * Recipient opened the email notification.
     * Detected via tracking pixel in HTML emails.
     */
    OPENED,

    /**
     * Recipient clicked a link within the email notification.
     * Detected via click tracking redirect URLs.
     */
    CLICKED,

    /**
     * Email delivery bounced (hard or soft bounce).
     * Indicates the recipient address was unreachable or rejected.
     */
    BOUNCED
}