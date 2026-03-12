/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.users;

import net.datafaker.Faker;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;

/**
 * Shared data generation for person-level PII fields.
 * All entity-specific generators delegate here for common fields.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class PersonDataGenerator {

    private final Faker faker;
    private final Random random;

    public PersonDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    public String firstName() {
        return faker.name().firstName();
    }

    public String lastName() {
        return faker.name().lastName();
    }

    public String email(String firstName, String lastName) {
        String localPart = (firstName.charAt(0) + lastName).toLowerCase().replaceAll("[^a-z0-9]", "");
        String domain = faker.internet().domainName().toLowerCase().replaceAll("[^a-z0-9.]", "");
        return localPart + "@" + domain;
    }

    public String username(String firstName, String lastName) {
        String base = (firstName.charAt(0) + lastName).toLowerCase().replaceAll("[^a-z0-9]", "");
        return base + faker.number().numberBetween(100, 999);
    }

    public String password() {
        String upper = faker.text().text(2, 3).toUpperCase().replaceAll("[^A-Z]", "AB");
        String lower = faker.text().text(4, 6).toLowerCase().replaceAll("[^a-z]", "xyz");
        String numbers = String.valueOf(faker.number().numberBetween(1000, 9999));
        String special = faker.options().option("@", "#", "$", "%", "!", "&");
        return upper + lower + numbers + special + faker.number().numberBetween(10, 99);
    }

    public String phoneNumber() {
        return faker.phoneNumber().phoneNumber();
    }

    public String address() {
        return faker.address().streetAddress() + ", " +
                faker.address().city() + ", " +
                faker.address().state();
    }

    public String zipCode() {
        return String.format("%05d", faker.number().numberBetween(10000, 99999));
    }

    public LocalDate birthdate(int minAge, int maxAge) {
        int age = faker.number().numberBetween(minAge, maxAge + 1);
        return LocalDate.now().minusYears(age)
                .minusDays(faker.number().numberBetween(0, 365));
    }

    public LocalDate entryDate() {
        int daysAgo = faker.number().numberBetween(0, 1825);
        return LocalDate.now().minusDays(daysAgo);
    }

    public JsonNullable<byte[]> profilePicture() {
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
        return JsonNullable.of(base64.getBytes());
    }

    public Faker faker() {
        return faker;
    }

    public Random random() {
        return random;
    }
}
