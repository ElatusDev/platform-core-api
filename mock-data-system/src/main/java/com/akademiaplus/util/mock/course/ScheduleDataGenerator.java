/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for schedule entities.
 */
@Component
public class ScheduleDataGenerator {

    private static final List<String> SCHEDULE_DAYS = Arrays.asList(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
    );

    private final Faker faker;
    private final Random random;

    public ScheduleDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String scheduleDay() {
        return SCHEDULE_DAYS.get(random.nextInt(SCHEDULE_DAYS.size()));
    }

    public LocalTime startTime() {
        int hour = faker.number().numberBetween(7, 18);
        int minute = faker.options().option(0, 15, 30, 45);
        return LocalTime.of(hour, minute);
    }

    public LocalTime endTime(LocalTime start) {
        int durationMinutes = faker.number().numberBetween(45, 120);
        return start.plusMinutes(durationMinutes);
    }
}
