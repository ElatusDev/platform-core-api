/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Generates fake data for tenant organizations.
 * Uses Mexican Spanish locale to match the platform's primary market.
 */
@Component
public class TenantDataGenerator {

    private final Faker faker;

    public TenantDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
    }

    public String organizationName() {
        return faker.company().name();
    }

    public String legalName() {
        return faker.company().name() + " S.A. de C.V.";
    }

    public String websiteUrl() {
        String domain = faker.internet().domainName().toLowerCase().replaceAll("[^a-z0-9.]", "");
        return "https://www." + domain;
    }

    public String email() {
        String domain = faker.internet().domainName().toLowerCase().replaceAll("[^a-z0-9.]", "");
        return "contacto@" + domain;
    }

    public String address() {
        return faker.address().streetAddress() + ", " +
                faker.address().city() + ", " +
                faker.address().state();
    }

    public String phone() {
        return faker.phoneNumber().cellPhone();
    }

    public String landline() {
        return faker.phoneNumber().phoneNumber();
    }

    public String description() {
        return faker.company().catchPhrase();
    }

    public String taxId() {
        String letters = faker.text().text(4, 4).toUpperCase().replaceAll("[^A-Z]", "ABCD");
        String numbers = String.valueOf(faker.number().numberBetween(100000, 999999));
        String checksum = faker.text().text(3, 3).toUpperCase().replaceAll("[^A-Z0-9]", "A1");
        return letters.substring(0, 4) + numbers + checksum.substring(0, 3);
    }
}
