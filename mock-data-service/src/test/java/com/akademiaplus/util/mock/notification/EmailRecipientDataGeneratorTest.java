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
 * Unit tests for {@link EmailRecipientDataGenerator}.
 */
@DisplayName("EmailRecipientDataGenerator")
class EmailRecipientDataGeneratorTest {

    private EmailRecipientDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new EmailRecipientDataGenerator();
    }

    @Nested
    @DisplayName("recipientEmail()")
    class RecipientEmail {

        @Test
        @DisplayName("Should generate non-blank email containing @")
        void shouldGenerateNonBlankEmail_containingAt() {
            // Given & When
            String email = generator.recipientEmail(0);

            // Then
            assertThat(email).isNotBlank();
            assertThat(email).contains("@");
        }

        @Test
        @DisplayName("Should generate unique emails for different indices")
        void shouldGenerateUniqueEmails_forDifferentIndices() {
            // Given & When
            String email0 = generator.recipientEmail(0);
            String email1 = generator.recipientEmail(1);

            // Then
            assertThat(email0).isNotEqualTo(email1);
        }

        @Test
        @DisplayName("Should include index prefix in generated email")
        void shouldIncludeIndexPrefix_inGeneratedEmail() {
            // Given & When
            String email = generator.recipientEmail(42);

            // Then
            assertThat(email).startsWith("recipient42.");
        }
    }
}
