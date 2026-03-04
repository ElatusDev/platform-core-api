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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates fake data for tenant billing cycles.
 */
@Component
public class TenantBillingCycleDataGenerator {

    private static final DateTimeFormatter BILLING_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Faker faker;

    public TenantBillingCycleDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    public String billingMonth() {
        int monthsAgo = faker.number().numberBetween(0, 12);
        return LocalDate.now().minusMonths(monthsAgo).format(BILLING_MONTH_FORMAT);
    }

    public LocalDate calculationDate() {
        int daysAgo = faker.number().numberBetween(0, 30);
        return LocalDate.now().minusDays(daysAgo);
    }

    public int userCount() {
        return faker.number().numberBetween(5, 200);
    }

    /**
     * Generates a random total billing amount between 500.00 and 50,000.00.
     *
     * @return total amount as a double
     */
    public double totalAmount() {
        return faker.number().randomDouble(2, 500, 50000);
    }

    public String notes() {
        return faker.lorem().sentence();
    }
}
