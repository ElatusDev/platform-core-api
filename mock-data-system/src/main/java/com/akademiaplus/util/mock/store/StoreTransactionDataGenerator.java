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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates fake data for store transaction entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class StoreTransactionDataGenerator {

    private static final List<String> TRANSACTION_TYPES = Arrays.asList(
            "SALE", "REFUND", "EXCHANGE"
    );

    private static final List<String> PAYMENT_METHODS = Arrays.asList(
            "CASH", "CREDIT_CARD", "DEBIT_CARD", "TRANSFER"
    );

    /** Minimum total amount for a mock transaction (inclusive). */
    public static final double MIN_TOTAL_AMOUNT = 10.0;

    /** Maximum total amount for a mock transaction (inclusive). */
    public static final double MAX_TOTAL_AMOUNT = 5000.0;

    /** Minimum number of sale items per transaction (inclusive). */
    public static final int MIN_SALE_ITEM_COUNT = 1;

    /** Maximum number of sale items per transaction (inclusive). */
    public static final int MAX_SALE_ITEM_COUNT = 5;

    /** Minimum store product ID for generated sale items (inclusive). */
    public static final long MIN_STORE_PRODUCT_ID = 1L;

    /** Maximum store product ID for generated sale items (inclusive). */
    public static final long MAX_STORE_PRODUCT_ID = 100L;

    /** Minimum quantity per sale item (inclusive). */
    public static final int MIN_SALE_ITEM_QUANTITY = 1;

    /** Maximum quantity per sale item (inclusive). */
    public static final int MAX_SALE_ITEM_QUANTITY = 20;

    private final Faker faker;
    private final Random random;

    public StoreTransactionDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String transactionType() {
        return TRANSACTION_TYPES.get(random.nextInt(TRANSACTION_TYPES.size()));
    }

    /**
     * Generates a random number of sale items per transaction.
     *
     * @return a value between {@value MIN_SALE_ITEM_COUNT} and {@value MAX_SALE_ITEM_COUNT}
     */
    public int saleItemCount() {
        return faker.number().numberBetween(MIN_SALE_ITEM_COUNT, MAX_SALE_ITEM_COUNT + 1);
    }

    /**
     * Generates a random store product ID for a sale item.
     *
     * @return a value between {@value MIN_STORE_PRODUCT_ID} and {@value MAX_STORE_PRODUCT_ID}
     */
    public Long saleItemStoreProductId() {
        return ThreadLocalRandom.current()
                .nextLong(MIN_STORE_PRODUCT_ID, MAX_STORE_PRODUCT_ID + 1);
    }

    /**
     * Generates a random quantity for a sale item.
     *
     * @return a value between {@value MIN_SALE_ITEM_QUANTITY} and {@value MAX_SALE_ITEM_QUANTITY}
     */
    public int saleItemQuantity() {
        return faker.number().numberBetween(MIN_SALE_ITEM_QUANTITY, MAX_SALE_ITEM_QUANTITY + 1);
    }

    public String paymentMethod() {
        return PAYMENT_METHODS.get(random.nextInt(PAYMENT_METHODS.size()));
    }

    /**
     * Generates a random total amount for a transaction.
     *
     * @return a value between {@value MIN_TOTAL_AMOUNT} and {@value MAX_TOTAL_AMOUNT}, rounded to 2 decimals
     */
    public Double totalAmount() {
        double amount = MIN_TOTAL_AMOUNT + (MAX_TOTAL_AMOUNT - MIN_TOTAL_AMOUNT) * random.nextDouble();
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
