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
import java.util.UUID;

/**
 * Generates fake data for card payment info entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class CardPaymentInfoDataGenerator {

    /** Supported card type values. */
    public static final List<String> CARD_TYPES = Arrays.asList(
            "visa", "mastercard", "amex", "discover"
    );

    private final Faker faker;
    private final Random random;

    public CardPaymentInfoDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates a tokenized card reference.
     *
     * @return a UUID-based token string
     */
    public String token() {
        return "tok_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generates a card type from the supported list.
     *
     * @return a card type string
     */
    public String cardType() {
        return CARD_TYPES.get(random.nextInt(CARD_TYPES.size()));
    }

    /**
     * Generates a synthetic payment ID reference.
     *
     * @return a positive long value
     */
    public Long paymentId() {
        return (long) faker.number().numberBetween(1, 10_000);
    }
}
