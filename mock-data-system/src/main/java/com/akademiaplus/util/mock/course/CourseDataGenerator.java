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

import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for course entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class CourseDataGenerator {

    private final Faker faker;
    private final Random random;

    public CourseDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String courseName() {
        return faker.educator().course();
    }

    public String courseDescription() {
        return faker.lorem().sentence(10);
    }

    public int maxCapacity() {
        return faker.number().numberBetween(10, 50);
    }
}
