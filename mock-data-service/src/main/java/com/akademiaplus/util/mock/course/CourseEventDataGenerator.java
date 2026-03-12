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

import java.time.LocalDate;
import java.util.Locale;

/**
 * Generates fake data for course event entities.
 */
@Component
public class CourseEventDataGenerator {

    private final Faker faker;

    public CourseEventDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    public LocalDate eventDate() {
        int daysOffset = faker.number().numberBetween(-30, 60);
        return LocalDate.now().plusDays(daysOffset);
    }

    public String eventTitle() {
        return faker.educator().course() + " - " + faker.lorem().word();
    }

    public String eventDescription() {
        return faker.lorem().sentence(8);
    }
}
