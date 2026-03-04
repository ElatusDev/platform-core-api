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
import java.util.UUID;

/**
 * Generates fake data for email attachment entities.
 */
@Component
public class EmailAttachmentDataGenerator {

    /** URL prefix used for all generated attachment URLs. */
    public static final String URL_PREFIX = "https://storage.akademiaplus.com/attachments/";

    private final Faker faker;

    public EmailAttachmentDataGenerator() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    /**
     * Generates a unique fake attachment URL.
     *
     * @param index ordinal used to guarantee uniqueness across a batch
     * @return a non-blank URL string
     */
    public String attachmentUrl(int index) {
        return URL_PREFIX + index + "/" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
    }
}
