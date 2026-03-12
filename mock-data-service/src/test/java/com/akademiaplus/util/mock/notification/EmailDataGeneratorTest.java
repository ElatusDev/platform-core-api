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
 * Unit tests for {@link EmailDataGenerator}.
 */
@DisplayName("EmailDataGenerator")
class EmailDataGeneratorTest {

    private EmailDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new EmailDataGenerator();
    }

    @Nested
    @DisplayName("subject()")
    class Subject {

        @Test
        @DisplayName("Should generate non-blank subject")
        void shouldGenerateNonBlankSubject() {
            // Given & When
            String subject = generator.subject();

            // Then
            assertThat(subject).isNotBlank();
        }
    }

    @Nested
    @DisplayName("body()")
    class Body {

        @Test
        @DisplayName("Should generate non-blank body")
        void shouldGenerateNonBlankBody() {
            // Given & When
            String body = generator.body();

            // Then
            assertThat(body).isNotBlank();
        }
    }

    @Nested
    @DisplayName("sender()")
    class Sender {

        @Test
        @DisplayName("Should generate non-blank sender containing @")
        void shouldGenerateNonBlankSender_containingAt() {
            // Given & When
            String sender = generator.sender();

            // Then
            assertThat(sender).isNotBlank();
            assertThat(sender).contains("@");
        }
    }
}
