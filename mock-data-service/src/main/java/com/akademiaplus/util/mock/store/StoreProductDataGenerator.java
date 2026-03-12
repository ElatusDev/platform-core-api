/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.store;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Generates fake data for store product entities.
 */
@Component
public class StoreProductDataGenerator {

    private final Faker faker;

    public StoreProductDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    public String name() {
        return faker.commerce().productName();
    }

    public String description() {
        return faker.lorem().sentence(8);
    }

    public double price() {
        return faker.number().numberBetween(10, 5000);
    }

    public int stockQuantity() {
        return faker.number().numberBetween(1, 200);
    }
}
