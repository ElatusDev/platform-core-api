/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

/**
 * Enumeration of available delivery channels for notifications.
 * Defines the different communication methods supported by the platform
 * for delivering notifications to users across various devices and services.
 */
public enum DeliveryChannel {
    /**
     * iOS push notifications delivered through Apple Push Notification Service (APNs).
     * Requires valid device tokens and proper APNs certificates.
     */
    IOS_PUSH,

    /**
     * Android push notifications delivered through Firebase Cloud Messaging (FCM).
     * Requires valid FCM registration tokens and Firebase project configuration.
     */
    ANDROID_PUSH,

    /**
     * Web application notifications for React-based frontend.
     * Delivered through WebSocket connections or browser push API.
     */
    WEBAPP,

    /**
     * SMS text messages delivered to mobile phone numbers.
     * Requires SMS service provider integration (e.g., Twilio, AWS SNS).
     */
    SMS,

    /**
     * Email notifications delivered to user email addresses.
     * Requires email service provider integration (e.g., SendGrid, AWS SES).
     */
    EMAIL
}