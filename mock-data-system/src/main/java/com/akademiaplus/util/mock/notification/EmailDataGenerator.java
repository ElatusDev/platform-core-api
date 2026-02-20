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
 * Generates fake data for email entities.
 */
@Component
public class EmailDataGenerator {

    private final Faker faker;

    public EmailDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
    }

    /**
     * Generates a fake email subject line.
     *
     * @return a non-blank subject string
     */
    public String subject() {
        return faker.lorem().sentence(4);
    }

    /**
     * Generates a fake email body.
     *
     * @return a non-blank body string
     */
    public String body() {
        return faker.lorem().paragraph(3);
    }

    /**
     * Generates a fake sender email address.
     *
     * @return a non-blank sender string
     */
    public String sender() {
        return faker.internet().emailAddress();
    }
}
