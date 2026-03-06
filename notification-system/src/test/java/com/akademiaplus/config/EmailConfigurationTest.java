/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailConfiguration")
class EmailConfigurationTest {

    private static final String TEST_HOST = "smtp.mailtrap.io";
    private static final int TEST_PORT = 587;
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-password";

    private EmailConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new EmailConfiguration();
    }

    @Nested
    @DisplayName("Bean Creation")
    class BeanCreation {

        @Test
        @DisplayName("Should create JavaMailSender with the provided SMTP properties")
        void shouldCreateJavaMailSender_whenPropertiesProvided() {
            // Given — configuration instance with test properties

            // When
            JavaMailSender mailSender = configuration.emailMailSender(
                    TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD);

            // Then
            assertThat(mailSender).isInstanceOf(JavaMailSenderImpl.class);
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            assertThat(impl.getHost()).isEqualTo(TEST_HOST);
            assertThat(impl.getPort()).isEqualTo(TEST_PORT);
            assertThat(impl.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(impl.getPassword()).isEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("Should configure SMTP transport protocol property")
        void shouldConfigureTransportProtocol() {
            // Given — configuration instance

            // When
            JavaMailSender mailSender = configuration.emailMailSender(
                    TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD);

            // Then
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            assertThat(impl.getJavaMailProperties().getProperty(
                    EmailConfiguration.PROP_TRANSPORT_PROTOCOL)).isEqualTo("smtp");
        }

        @Test
        @DisplayName("Should configure SMTP authentication property")
        void shouldConfigureSmtpAuth() {
            // Given — configuration instance

            // When
            JavaMailSender mailSender = configuration.emailMailSender(
                    TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD);

            // Then
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            assertThat(impl.getJavaMailProperties().getProperty(
                    EmailConfiguration.PROP_SMTP_AUTH)).isEqualTo("true");
        }

        @Test
        @DisplayName("Should configure STARTTLS enablement and requirement properties")
        void shouldConfigureStarttlsProperties() {
            // Given — configuration instance

            // When
            JavaMailSender mailSender = configuration.emailMailSender(
                    TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD);

            // Then
            JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
            assertThat(impl.getJavaMailProperties().getProperty(
                    EmailConfiguration.PROP_SMTP_STARTTLS_ENABLE)).isEqualTo("true");
            assertThat(impl.getJavaMailProperties().getProperty(
                    EmailConfiguration.PROP_SMTP_STARTTLS_REQUIRED)).isEqualTo("true");
        }
    }
}
