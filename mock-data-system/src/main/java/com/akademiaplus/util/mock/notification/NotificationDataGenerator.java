/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for notification entities.
 */
@Component
public class NotificationDataGenerator {

    private static final List<String> NOTIFICATION_TYPES = Arrays.asList(
            "COURSE_REMINDER", "PAYMENT_DUE", "ENROLLMENT_CONFIRMATION",
            "SCHEDULE_CHANGE", "SYSTEM_MAINTENANCE", "PROMOTIONAL",
            "ANNOUNCEMENT", "ASSIGNMENT_REMINDER", "GRADE_NOTIFICATION"
    );

    private static final List<String> PRIORITIES = Arrays.asList(
            "LOW", "NORMAL", "HIGH", "URGENT"
    );

    private final Faker faker;
    private final Random random;

    public NotificationDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String title() {
        return faker.lorem().sentence(3);
    }

    public String content() {
        return faker.lorem().paragraph(2);
    }

    public String type() {
        return NOTIFICATION_TYPES.get(random.nextInt(NOTIFICATION_TYPES.size()));
    }

    public String priority() {
        return PRIORITIES.get(random.nextInt(PRIORITIES.size()));
    }

    public OffsetDateTime scheduledAt() {
        int daysOffset = faker.number().numberBetween(1, 30);
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(daysOffset);
    }

    public OffsetDateTime expiresAt(OffsetDateTime scheduledAt) {
        int daysAfter = faker.number().numberBetween(7, 90);
        return scheduledAt.plusDays(daysAfter);
    }
}
