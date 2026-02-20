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
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Generates fake data for notification delivery entities.
 */
@Component
public class NotificationDeliveryDataGenerator {

    private final Faker faker;
    private final Random random;

    public NotificationDeliveryDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates a random delivery channel.
     *
     * @return one of the {@link DeliveryChannel} values
     */
    public DeliveryChannel channel() {
        DeliveryChannel[] values = DeliveryChannel.values();
        return values[random.nextInt(values.length)];
    }

    /**
     * Generates a random delivery status.
     *
     * @return one of the {@link DeliveryStatus} values
     */
    public DeliveryStatus status() {
        DeliveryStatus[] values = DeliveryStatus.values();
        return values[random.nextInt(values.length)];
    }

    /**
     * Generates a recipient identifier appropriate for the channel.
     *
     * @return a fake email, phone, or device token
     */
    public String recipientIdentifier() {
        return faker.internet().emailAddress();
    }

    /**
     * Generates a sent-at timestamp within the last 30 days.
     *
     * @return a recent {@link LocalDateTime}
     */
    public LocalDateTime sentAt() {
        int daysAgo = faker.number().numberBetween(0, 30);
        return LocalDateTime.now().minusDays(daysAgo);
    }

    /**
     * Generates a retry count.
     *
     * @return a value between 0 and 3
     */
    public int retryCount() {
        return faker.number().numberBetween(0, 4);
    }

    /**
     * Generates an external tracking ID.
     *
     * @return a UUID-based external ID
     */
    public String externalId() {
        return "ext_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
