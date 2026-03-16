/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates fake data for email template entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class EmailTemplateDataGenerator {

    private static final String SUBJECT_PREFIX = "{{orgName}} - ";
    private static final String HTML_WRAPPER_OPEN = "<html><body><h1>";
    private static final String HTML_WRAPPER_MID = "</h1><p>";
    private static final String HTML_WRAPPER_CLOSE = "</p></body></html>";
    private static final double ACTIVE_PROBABILITY = 0.9;

    private static final String[] TEMPLATE_NAMES = {
            "welcome", "password-reset", "payment-receipt",
            "enrollment-confirmation", "class-reminder"
    };

    private static final String[] CATEGORIES = {
            "AUTHENTICATION", "BILLING", "ENROLLMENT", "NOTIFICATION"
    };

    private final Faker faker;
    private final Random random;
    private final AtomicInteger nameCounter = new AtomicInteger(0);
    private final AtomicInteger categoryCounter = new AtomicInteger(0);

    public EmailTemplateDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    /**
     * Returns a template name, cycling through the predefined list.
     *
     * @return the next template name in the cycle
     */
    public String templateName() {
        return TEMPLATE_NAMES[nameCounter.getAndIncrement() % TEMPLATE_NAMES.length];
    }

    /**
     * Generates a description for the email template.
     *
     * @return a short lorem ipsum sentence
     */
    public String description() {
        return faker.lorem().sentence(6);
    }

    /**
     * Returns a category, cycling through the predefined list.
     *
     * @return the next category in the cycle
     */
    public String category() {
        return CATEGORIES[categoryCounter.getAndIncrement() % CATEGORIES.length];
    }

    /**
     * Generates an email subject template with an organization name placeholder.
     *
     * @return a subject template string with a {@code {{orgName}}} prefix
     */
    public String subjectTemplate() {
        return SUBJECT_PREFIX + faker.lorem().sentence(3);
    }

    /**
     * Generates a simple HTML body template.
     *
     * @return an HTML string wrapping a title and paragraph
     */
    public String bodyHtml() {
        return HTML_WRAPPER_OPEN
                + faker.lorem().sentence(3)
                + HTML_WRAPPER_MID
                + faker.lorem().paragraph(2)
                + HTML_WRAPPER_CLOSE;
    }

    /**
     * Generates a plain-text body template.
     *
     * @return a plain-text paragraph
     */
    public String bodyText() {
        return faker.lorem().paragraph(2);
    }

    /**
     * Returns whether the template should be active, with a 90% probability of true.
     *
     * @return {@code true} approximately 90% of the time
     */
    public boolean isActive() {
        return random.nextDouble() < ACTIVE_PROBABILITY;
    }
}
