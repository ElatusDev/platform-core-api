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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Generates fake data for store sale item entities.
 */
@Component
public class StoreSaleItemDataGenerator {

    /** Minimum quantity per sale item (inclusive). */
    public static final int MIN_QUANTITY = 1;

    /** Maximum quantity per sale item (inclusive). */
    public static final int MAX_QUANTITY = 20;

    /** Minimum unit price in whole currency units (inclusive). */
    public static final int MIN_PRICE = 10;

    /** Maximum unit price in whole currency units (inclusive). */
    public static final int MAX_PRICE = 5000;

    private final Faker faker;

    public StoreSaleItemDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    /**
     * Generates a random quantity for the sale item.
     *
     * @return a value between {@value MIN_QUANTITY} and {@value MAX_QUANTITY}
     */
    public int quantity() {
        return faker.number().numberBetween(MIN_QUANTITY, MAX_QUANTITY + 1);
    }

    /**
     * Generates a random unit price at the time of sale.
     *
     * @return a BigDecimal with scale 2
     */
    public BigDecimal unitPriceAtSale() {
        double price = faker.number().numberBetween(MIN_PRICE, MAX_PRICE + 1);
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }
}
