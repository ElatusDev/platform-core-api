/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for compensation entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class CompensationDataGenerator {

    private static final List<String> COMPENSATION_TYPES = Arrays.asList(
            "hourly", "salary", "commission", "contract", "bonus"
    );

    private final Faker faker;
    private final Random random;

    public CompensationDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String compensationType() {
        return COMPENSATION_TYPES.get(random.nextInt(COMPENSATION_TYPES.size()));
    }

    public double amount() {
        return faker.number().numberBetween(5000, 50000);
    }
}
