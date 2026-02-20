/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmailAttachmentDataGenerator}.
 */
@DisplayName("EmailAttachmentDataGenerator")
class EmailAttachmentDataGeneratorTest {

    private EmailAttachmentDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new EmailAttachmentDataGenerator();
    }

    @Nested
    @DisplayName("attachmentUrl()")
    class AttachmentUrl {

        @Test
        @DisplayName("Should generate non-blank URL starting with storage prefix")
        void shouldGenerateNonBlankUrl_startingWithStoragePrefix() {
            // Given & When
            String url = generator.attachmentUrl(0);

            // Then
            assertThat(url).isNotBlank();
            assertThat(url).startsWith(EmailAttachmentDataGenerator.URL_PREFIX);
        }

        @Test
        @DisplayName("Should generate unique URLs for different indices")
        void shouldGenerateUniqueUrls_forDifferentIndices() {
            // Given & When
            String url0 = generator.attachmentUrl(0);
            String url1 = generator.attachmentUrl(1);

            // Then
            assertThat(url0).isNotEqualTo(url1);
        }

        @Test
        @DisplayName("Should generate URL ending with .pdf extension")
        void shouldGenerateUrl_endingWithPdfExtension() {
            // Given & When
            String url = generator.attachmentUrl(0);

            // Then
            assertThat(url).endsWith(".pdf");
        }
    }
}
