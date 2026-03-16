/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates fake data for tenant branding configuration entities.
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class TenantBrandingDataGenerator {

    private static final String LOGO_URL_PREFIX = "https://example.com/logos/";
    private static final String LOGO_URL_SUFFIX = ".png";
    private static final String ACADEMY_SUFFIX = " Academy";
    private static final String HEX_COLOR_FORMAT = "#%06x";
    private static final int MAX_COLOR_VALUE = 0xFFFFFF + 1;

    private static final List<String> FONT_FAMILIES = List.of(
            "Roboto", "Open Sans", "Lato", "Montserrat", "Poppins"
    );

    private final Faker faker;
    private final Random random;
    private final AtomicInteger fontCounter = new AtomicInteger(0);

    public TenantBrandingDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates a school name by appending "Academy" to a fake company name.
     *
     * @return a generated school name
     */
    public String schoolName() {
        return faker.company().name() + ACADEMY_SUFFIX;
    }

    /**
     * Generates a fake logo URL using a slug-based path.
     *
     * @return a generated logo URL
     */
    public String logoUrl() {
        return LOGO_URL_PREFIX + faker.internet().slug() + LOGO_URL_SUFFIX;
    }

    /**
     * Generates a random hex color string.
     *
     * @return a hex color in the format "#RRGGBB"
     */
    public String primaryColor() {
        return String.format(HEX_COLOR_FORMAT, random.nextInt(MAX_COLOR_VALUE));
    }

    /**
     * Generates a random hex color string for the secondary color.
     *
     * @return a hex color in the format "#RRGGBB"
     */
    public String secondaryColor() {
        return String.format(HEX_COLOR_FORMAT, random.nextInt(MAX_COLOR_VALUE));
    }

    /**
     * Returns a font family name, cycling through a predefined list.
     *
     * @return a CSS font family name
     */
    public String fontFamily() {
        return FONT_FAMILIES.get(fontCounter.getAndIncrement() % FONT_FAMILIES.size());
    }
}
