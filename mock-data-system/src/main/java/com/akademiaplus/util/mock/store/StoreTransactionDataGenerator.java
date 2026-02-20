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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates fake data for store transaction entities.
 */
@Component
public class StoreTransactionDataGenerator {

    private static final List<String> TRANSACTION_TYPES = Arrays.asList(
            "SALE", "REFUND", "EXCHANGE"
    );

    private static final List<String> PAYMENT_METHODS = Arrays.asList(
            "CASH", "CREDIT_CARD", "DEBIT_CARD", "TRANSFER"
    );

    private final Faker faker;
    private final Random random;

    public StoreTransactionDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String transactionType() {
        return TRANSACTION_TYPES.get(random.nextInt(TRANSACTION_TYPES.size()));
    }

    public double totalAmount() {
        return faker.number().numberBetween(50, 10000);
    }

    public String paymentMethod() {
        return PAYMENT_METHODS.get(random.nextInt(PAYMENT_METHODS.size()));
    }
}
