/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.newsfeed.NewsFeedStatus;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates fake data for news feed item entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class NewsFeedItemDataGenerator {

    private static final String IMAGE_URL_PREFIX = "https://example.com/news/";
    private static final String IMAGE_URL_SUFFIX = ".jpg";
    private static final double IMAGE_PROBABILITY = 0.5;

    private static final NewsFeedStatus[] STATUSES = {
            NewsFeedStatus.DRAFT, NewsFeedStatus.PUBLISHED
    };

    private final Faker faker;
    private final Random random;
    private final AtomicInteger statusCounter = new AtomicInteger(0);

    public NewsFeedItemDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates a news feed item title.
     *
     * @return a sentence with five words
     */
    public String title() {
        return faker.lorem().sentence(5);
    }

    /**
     * Generates a news feed item body.
     *
     * @return three paragraphs of lorem ipsum text
     */
    public String body() {
        return faker.lorem().paragraph(3);
    }

    /**
     * Generates an optional image URL with a 50% probability of being null.
     *
     * @return an image URL or {@code null}
     */
    public String imageUrl() {
        if (random.nextDouble() < IMAGE_PROBABILITY) {
            return null;
        }
        return IMAGE_URL_PREFIX + faker.internet().slug() + IMAGE_URL_SUFFIX;
    }

    /**
     * Returns a news feed status, cycling between DRAFT and PUBLISHED.
     *
     * @return the next {@link NewsFeedStatus} in the cycle
     */
    public NewsFeedStatus status() {
        return STATUSES[statusCounter.getAndIncrement() % STATUSES.length];
    }
}
