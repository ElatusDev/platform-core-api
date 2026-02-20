/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link NotificationCreationRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class NotificationFactory implements DataFactory<NotificationCreationRequestDTO> {

    private final NotificationDataGenerator generator;

    @Override
    public List<NotificationCreationRequestDTO> generate(int count) {
        List<NotificationCreationRequestDTO> notifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            notifications.add(createNotification());
        }
        return notifications;
    }

    private NotificationCreationRequestDTO createNotification() {
        OffsetDateTime scheduledAt = generator.scheduledAt();
        NotificationCreationRequestDTO dto = new NotificationCreationRequestDTO();
        dto.setTitle(generator.title());
        dto.setContent(generator.content());
        dto.setType(generator.type());
        dto.setPriority(generator.priority());
        dto.setScheduledAt(scheduledAt);
        dto.setExpiresAt(generator.expiresAt(scheduledAt));
        return dto;
    }
}
