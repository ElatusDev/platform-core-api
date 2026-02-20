/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link NotificationDeliveryRequest} instances with fake data.
 *
 * <p>Requires notification IDs to be injected via setter before
 * {@link #generate(int)} is called.</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationDeliveryFactory
        implements DataFactory<NotificationDeliveryFactory.NotificationDeliveryRequest> {

    /** Error message when notification IDs have not been set. */
    public static final String ERROR_NOTIFICATION_IDS_NOT_SET =
            "availableNotificationIds must be set before generating deliveries";

    private final NotificationDeliveryDataGenerator generator;

    @Setter
    private List<Long> availableNotificationIds = List.of();

    @Override
    public List<NotificationDeliveryRequest> generate(int count) {
        if (availableNotificationIds.isEmpty()) {
            throw new IllegalStateException(ERROR_NOTIFICATION_IDS_NOT_SET);
        }
        List<NotificationDeliveryRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long notificationId = availableNotificationIds
                    .get(i % availableNotificationIds.size());
            items.add(createRequest(notificationId));
        }
        return items;
    }

    private NotificationDeliveryRequest createRequest(Long notificationId) {
        return new NotificationDeliveryRequest(
                notificationId,
                generator.channel(),
                generator.recipientIdentifier(),
                generator.status(),
                generator.sentAt(),
                generator.retryCount(),
                generator.externalId()
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param notificationId      FK to the parent notification
     * @param channel             the delivery channel
     * @param recipientIdentifier the recipient address/token
     * @param status              the delivery status
     * @param sentAt              when the delivery was sent
     * @param retryCount          number of retries
     * @param externalId          external tracking ID
     */
    public record NotificationDeliveryRequest(
            Long notificationId,
            DeliveryChannel channel,
            String recipientIdentifier,
            DeliveryStatus status,
            LocalDateTime sentAt,
            int retryCount,
            String externalId) { }
}
