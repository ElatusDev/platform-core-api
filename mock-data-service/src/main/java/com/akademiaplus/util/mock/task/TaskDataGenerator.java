/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.task;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates fake data for task entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class TaskDataGenerator {

    private static final String ASSIGNEE_TYPE_EMPLOYEE = "EMPLOYEE";
    private static final double DESCRIPTION_NULL_PROBABILITY = 0.3;
    private static final int MIN_DUE_DAYS = 1;
    private static final int MAX_DUE_DAYS = 60;

    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH"};
    private static final String[] STATUSES = {"PENDING", "IN_PROGRESS", "COMPLETED"};

    private final Faker faker;
    private final Random random;
    private final AtomicInteger priorityCounter = new AtomicInteger(0);
    private final AtomicInteger statusCounter = new AtomicInteger(0);

    public TaskDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates a task title.
     *
     * @return a sentence with four words
     */
    public String title() {
        return faker.lorem().sentence(4);
    }

    /**
     * Generates an optional task description with a 30% probability of being null.
     *
     * @return two paragraphs of lorem ipsum text or {@code null}
     */
    public String description() {
        if (random.nextDouble() < DESCRIPTION_NULL_PROBABILITY) {
            return null;
        }
        return faker.lorem().paragraph(2);
    }

    /**
     * Generates a due date between 1 and 60 days from now.
     *
     * @return a future {@link LocalDate}
     */
    public LocalDate dueDate() {
        return LocalDate.now().plusDays(faker.number().numberBetween(MIN_DUE_DAYS, MAX_DUE_DAYS));
    }

    /**
     * Returns a priority value, cycling through LOW, MEDIUM, and HIGH.
     *
     * @return the next priority in the cycle
     */
    public String priority() {
        return PRIORITIES[priorityCounter.getAndIncrement() % PRIORITIES.length];
    }

    /**
     * Returns a status value, cycling through PENDING, IN_PROGRESS, and COMPLETED.
     *
     * @return the next status in the cycle
     */
    public String status() {
        return STATUSES[statusCounter.getAndIncrement() % STATUSES.length];
    }

    /**
     * Returns the fixed assignee type for generated tasks.
     *
     * @return the string "EMPLOYEE"
     */
    public String assigneeType() {
        return ASSIGNEE_TYPE_EMPLOYEE;
    }
}
