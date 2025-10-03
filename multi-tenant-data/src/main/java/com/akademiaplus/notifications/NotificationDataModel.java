/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a notification in the multi-tenant platform.
 * Stores notification content and metadata for delivery across multiple channels
 * including mobile apps, web applications, SMS, and email.
 * <p>
 * Notifications can be scheduled for future delivery and have expiration dates
 * to ensure relevant and timely communications with platform users.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "notifications")
@SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(NotificationDataModel.NotificationCompositeId.class)
public class NotificationDataModel extends TenantScoped {

    /**
     * Unique identifier for the notification within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "notification_id")
    private Integer notificationId;

    /**
     * Title of the notification displayed to users.
     * Brief, descriptive header that summarizes the notification purpose.
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Main content body of the notification.
     * Contains the detailed message or information for the user.
     * <p>
     * Stored as TEXT to accommodate longer notification messages.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Type classification of the notification.
     * Used for categorization, filtering, and delivery rule processing.
     * <p>
     * Examples: COURSE_REMINDER, PAYMENT_DUE, ENROLLMENT_CONFIRMATION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType type;

    /**
     * Priority level of the notification.
     * Determines delivery urgency and user interface treatment.
     * <p>
     * Higher priority notifications may bypass user preferences or retry more aggressively.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationPriority priority;

    /**
     * Scheduled delivery time for the notification.
     * If null, the notification should be delivered immediately.
     * <p>
     * Used for time-sensitive notifications like course reminders or payment deadlines.
     */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * Expiration time after which the notification should not be delivered.
     * Prevents delivery of outdated or irrelevant information.
     * <p>
     * If null, the notification does not expire and remains valid indefinitely.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Optional reference to the user who should receive this notification.
     * If null, the notification may be targeted through other mechanisms.
     * <p>
     * Used for direct user targeting and personalized notifications.
     */
    @Column(name = "target_user_id")
    private Integer targetUserId;

    /**
     * Optional metadata in JSON format for additional notification properties.
     * Can store channel-specific configurations, action buttons, deep links, etc.
     * <p>
     * Provides flexibility for different notification types and delivery channels.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Composite primary key class for Notification entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationCompositeId implements Serializable {
        private Integer tenantId;
        private Integer notificationId;
    }
}