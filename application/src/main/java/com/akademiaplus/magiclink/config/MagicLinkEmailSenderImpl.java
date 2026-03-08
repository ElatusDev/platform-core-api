/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.config;

import com.akademiaplus.magiclink.usecases.MagicLinkEmailSender;
import com.akademiaplus.notification.usecases.EmailDeliveryChannelStrategy;
import com.akademiaplus.notification.usecases.DeliveryResult;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationPriority;
import com.akademiaplus.notifications.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link MagicLinkEmailSender} using the notification
 * system's {@link EmailDeliveryChannelStrategy}.
 *
 * <p>Lives in the application module where both security and
 * notification-system dependencies are available.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Slf4j
@Service
public class MagicLinkEmailSenderImpl implements MagicLinkEmailSender {

    private final EmailDeliveryChannelStrategy emailDeliveryChannelStrategy;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the sender with email delivery infrastructure.
     *
     * @param emailDeliveryChannelStrategy the email delivery strategy
     * @param applicationContext           the Spring application context
     */
    public MagicLinkEmailSenderImpl(EmailDeliveryChannelStrategy emailDeliveryChannelStrategy,
                                     ApplicationContext applicationContext) {
        this.emailDeliveryChannelStrategy = emailDeliveryChannelStrategy;
        this.applicationContext = applicationContext;
    }

    @Override
    public void send(String recipientEmail, String subject, String htmlContent) {
        NotificationDataModel notification = applicationContext.getBean(NotificationDataModel.class);
        notification.setTitle(subject);
        notification.setContent(htmlContent);
        notification.setType(NotificationType.ANNOUNCEMENT);
        notification.setPriority(NotificationPriority.HIGH);

        DeliveryResult result = emailDeliveryChannelStrategy.deliver(notification, recipientEmail);
        if (result.status() != DeliveryStatus.SENT) {
            log.warn("Magic link email delivery failed for {}: {}", recipientEmail, result.failureReason());
        }
    }
}
