/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import net.datafaker.Faker;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for tenant subscriptions.
 */
@Component
public class TenantSubscriptionDataGenerator {

    private static final List<SubscriptionCreateRequestDTO.TypeEnum> SUBSCRIPTION_TYPES = Arrays.asList(
            SubscriptionCreateRequestDTO.TypeEnum.BASIC,
            SubscriptionCreateRequestDTO.TypeEnum.STANDARD,
            SubscriptionCreateRequestDTO.TypeEnum.PREMIUM,
            SubscriptionCreateRequestDTO.TypeEnum.ENTERPRISE
    );

    private final Faker faker;
    private final Random random;

    public TenantSubscriptionDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public SubscriptionCreateRequestDTO.TypeEnum type() {
        return SUBSCRIPTION_TYPES.get(random.nextInt(SUBSCRIPTION_TYPES.size()));
    }

    public JsonNullable<Integer> maxUsers() {
        return JsonNullable.of(faker.number().numberBetween(10, 500));
    }

    public LocalDate billingDate() {
        return LocalDate.now().plusDays(faker.number().numberBetween(1, 30));
    }

    public double ratePerStudent() {
        return faker.number().numberBetween(50, 500);
    }
}
