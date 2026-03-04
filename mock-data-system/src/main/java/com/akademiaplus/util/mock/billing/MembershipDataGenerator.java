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
 * Generates fake data for membership entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class MembershipDataGenerator {

    private static final List<String> MEMBERSHIP_TYPES = Arrays.asList(
            "monthly", "quarterly", "semiannual", "annual"
    );

    private final Faker faker;
    private final Random random;

    public MembershipDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String membershipType() {
        return MEMBERSHIP_TYPES.get(random.nextInt(MEMBERSHIP_TYPES.size()));
    }

    public double fee() {
        return faker.number().numberBetween(500, 5000);
    }

    public String description() {
        return faker.lorem().sentence(5);
    }
}
