/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a delivery attempt for a notification through a specific channel.
 * Tracks the status, timing, and results of notification delivery attempts
 * across multiple channels including push notifications, web, SMS, and email.
 * <p>
 * Each notification can have multiple delivery records for different channels
 * and retry attempts, providing comprehensive delivery tracking and analytics.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "notification_deliveries")
@SQLDelete(sql = "UPDATE notification_deliveries SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(NotificationDeliveryDataModel.NotificationDeliveryCompositeId.class)
public class NotificationDeliveryDataModel extends TenantScoped {

    /**
     * Unique identifier for the delivery attempt within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "notification_delivery_id")
    private Long notificationDeliveryId;

    /**
     * Reference to the notification being delivered.
     * Links this delivery attempt to the original notification content.
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /**
     * Delivery channel used for this notification attempt.
     * Determines the technology and format used for delivery.
     * <p>
     * Examples: IOS_PUSH, ANDROID_PUSH, WEBAPP, SMS, EMAIL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private DeliveryChannel channel;

    /**
     * Identifier for the recipient on the specific channel.
     * Format varies by channel type (device token, email address, phone number, user ID).
     * <p>
     * Examples: FCM token for push notifications, email address for email delivery,
     * phone number for SMS, or user ID for web app notifications.
     */
    @Column(name = "recipient_identifier", nullable = false, length = 500)
    private String recipientIdentifier;

    /**
     * Current status of the delivery attempt.
     * Tracks the progression from initial attempt through final delivery or failure.
     * <p>
     * Used for monitoring delivery success rates and identifying delivery issues.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    /**
     * Timestamp when the notification was sent to the delivery service.
     * Marks when the platform initiated the delivery attempt.
     * <p>
     * Used for delivery timing analysis and SLA monitoring.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Timestamp when delivery confirmation was received.
     * Indicates successful delivery to the recipient's device or service.
     * <p>
     * May be null for channels that don't provide delivery confirmations.
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp when the recipient acknowledged or interacted with the notification.
     * Indicates the notification was actually seen or acted upon by the user.
     * <p>
     * Used for engagement analytics and notification effectiveness measurement.
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /**
     * Reason for delivery failure if the attempt was unsuccessful.
     * Provides diagnostic information for troubleshooting and monitoring.
     * <p>
     * Examples: "Invalid device token", "Email address not found", "Rate limit exceeded"
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Number of retry attempts made for this delivery.
     * Used for retry logic and preventing infinite retry loops.
     * <p>
     * Incremented with each retry attempt up to a maximum threshold.
     */
    @Column(name = "retry_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer retryCount;

    /**
     * External identifier from the delivery service provider.
     * Reference ID returned by services like FCM, SendGrid, Twilio, etc.
     * <p>
     * Used for tracking delivery status with external providers and debugging issues.
     */
    @Column(name = "external_id", length = 255)
    private String externalId;

    /**
     * Composite primary key class for NotificationDelivery entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationDeliveryCompositeId implements Serializable {
        private Long tenantId;
        private Long notificationDeliveryId;
    }
}