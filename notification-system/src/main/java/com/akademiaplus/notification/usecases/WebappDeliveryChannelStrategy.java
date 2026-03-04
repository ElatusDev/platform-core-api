/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDataModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

/**
 * Delivers notifications via Server-Sent Events (SSE) to connected webapp clients.
 * <p>
 * Looks up the target user's active {@link SseEmitter} from the
 * {@link SseEmitterRegistry} and sends the notification as an SSE event.
 * Returns {@link DeliveryResult#failed(String)} if the user is not connected
 * or if the send operation fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebappDeliveryChannelStrategy implements DeliveryChannelStrategy {

    /** SSE event name used for notification events. */
    public static final String EVENT_NAME = "notification";

    /** Error message when the target user has no active SSE connection. */
    public static final String ERROR_USER_NOT_CONNECTED = "User not connected via SSE";

    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.WEBAPP;
    }

    @Override
    public DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier) {
        final Long tenantId = notification.getTenantId();
        final Long userId = Long.parseLong(recipientIdentifier);

        Optional<SseEmitter> emitterOpt = sseEmitterRegistry.getEmitter(tenantId, userId);

        if (emitterOpt.isEmpty()) {
            return DeliveryResult.failed(ERROR_USER_NOT_CONNECTED);
        }

        try {
            emitterOpt.get().send(
                    SseEmitter.event()
                            .name(EVENT_NAME)
                            .data(notification)
            );
            return DeliveryResult.sent();
        } catch (IOException e) {
            log.warn("SSE send failed for tenant {} user {}: {}", tenantId, userId, e.getMessage());
            sseEmitterRegistry.remove(tenantId, userId);
            return DeliveryResult.failed(e.getMessage());
        }
    }
}
