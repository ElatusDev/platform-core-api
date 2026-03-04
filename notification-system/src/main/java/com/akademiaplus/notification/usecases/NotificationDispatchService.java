/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates notification delivery through the appropriate
 * {@link DeliveryChannelStrategy}.
 * <p>
 * Resolves the strategy for the target channel, executes delivery,
 * and persists a {@link NotificationDeliveryDataModel} record with
 * the outcome. Currently dispatches via the WEBAPP (SSE) channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    /** Error message when no strategy is registered for a channel. */
    public static final String ERROR_NO_STRATEGY = "No delivery strategy registered for channel: %s";

    /** Error message when notification has no target user. */
    public static final String ERROR_TARGET_USER_REQUIRED = "Notification targetUserId is required for dispatch";

    /** Initial retry count for new delivery records. */
    public static final int INITIAL_RETRY_COUNT = 0;

    private final ApplicationContext applicationContext;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final List<DeliveryChannelStrategy> strategies;

    private Map<DeliveryChannel, DeliveryChannelStrategy> strategyMap;

    /**
     * Builds the channel-to-strategy lookup map from all injected strategy beans.
     */
    @PostConstruct
    void initStrategyMap() {
        strategyMap = new EnumMap<>(DeliveryChannel.class);
        for (DeliveryChannelStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannel(), strategy);
        }
    }

    /**
     * Dispatches a notification via the WEBAPP channel.
     * <p>
     * Resolves the {@link WebappDeliveryChannelStrategy}, executes delivery,
     * and persists a {@link NotificationDeliveryDataModel} with the outcome.
     * The delivery entity is created via {@link ApplicationContext#getBean}
     * (prototype scope) and its ID is assigned by {@code EntityIdAssigner}.
     *
     * @param notification the notification to dispatch
     * @return the saved delivery record
     * @throws IllegalArgumentException if the notification has no targetUserId
     * @throws IllegalStateException    if no strategy is registered for WEBAPP
     */
    @Transactional
    public NotificationDeliveryDataModel dispatch(NotificationDataModel notification) {
        if (notification.getTargetUserId() == null) {
            throw new IllegalArgumentException(ERROR_TARGET_USER_REQUIRED);
        }

        final DeliveryChannel channel = DeliveryChannel.WEBAPP;
        final DeliveryChannelStrategy strategy = resolveStrategy(channel);
        final String recipientIdentifier = String.valueOf(notification.getTargetUserId());

        DeliveryResult result = strategy.deliver(notification, recipientIdentifier);

        return persistDeliveryRecord(notification, channel, recipientIdentifier, result);
    }

    /**
     * Resolves the strategy for the given delivery channel.
     *
     * @param channel the target channel
     * @return the strategy implementation
     * @throws IllegalStateException if no strategy is registered
     */
    DeliveryChannelStrategy resolveStrategy(DeliveryChannel channel) {
        DeliveryChannelStrategy strategy = strategyMap.get(channel);
        if (strategy == null) {
            throw new IllegalStateException(String.format(ERROR_NO_STRATEGY, channel));
        }
        return strategy;
    }

    private NotificationDeliveryDataModel persistDeliveryRecord(
            NotificationDataModel notification,
            DeliveryChannel channel,
            String recipientIdentifier,
            DeliveryResult result) {

        final NotificationDeliveryDataModel delivery =
                applicationContext.getBean(NotificationDeliveryDataModel.class);

        delivery.setNotificationId(notification.getNotificationId());
        delivery.setChannel(channel);
        delivery.setRecipientIdentifier(recipientIdentifier);
        delivery.setStatus(result.status());
        delivery.setRetryCount(INITIAL_RETRY_COUNT);

        if (result.status() == DeliveryStatus.SENT) {
            delivery.setSentAt(LocalDateTime.now());
        }
        if (result.failureReason() != null) {
            delivery.setFailureReason(result.failureReason());
        }

        return notificationDeliveryRepository.save(delivery);
    }
}
