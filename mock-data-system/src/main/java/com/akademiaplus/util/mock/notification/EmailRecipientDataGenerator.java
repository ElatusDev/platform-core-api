/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Generates fake data for email recipient entities.
 */
@Component
public class EmailRecipientDataGenerator {

    private final Faker faker;

    public EmailRecipientDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
    }

    /**
     * Generates a unique fake recipient email address.
     *
     * @param index ordinal used to guarantee uniqueness across a batch
     * @return a non-blank email address
     */
    public String recipientEmail(int index) {
        return "recipient" + index + "." + faker.internet().emailAddress();
    }
}
