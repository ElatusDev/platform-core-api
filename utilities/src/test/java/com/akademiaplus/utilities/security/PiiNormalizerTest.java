/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.security;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.akademiaplus.utilities.exceptions.security.ErrorNormalizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PiiNormalizer}.
 * Tests email and phone number normalization according to international standards.
 *
 * <p>Tests follow the Given-When-Then pattern:
 * <ul>
 *   <li><b>Given:</b> Test setup and preconditions</li>
 *   <li><b>When:</b> Execution of the method under test</li>
 *   <li><b>Then:</b> Verification of expected outcomes</li>
 * </ul>
 *
 * @author ElatusDev
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PiiNormalizer Tests")
class PiiNormalizerTest {

    // Shared constants - used across multiple tests
    private static final String DEFAULT_REGION_CODE = "US";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_EMAIL_WITH_UPPERCASE = "Test@Example.COM";
    private static final String VALID_EMAIL_WITH_WHITESPACE = "  test@example.com  ";
    private static final String VALID_PHONE_NUMBER = "+1234567890";
    private static final String NATIONAL_FORMAT_PHONE = "(123) 456-7890";

    // Error message fragments for assertions
    private static final String ERROR_MESSAGE_NULL_OR_EMPTY = "cannot be null or empty";
    private static final String ERROR_MESSAGE_INVALID_FORMAT = "format is invalid";
    private static final String ERROR_MESSAGE_PARSE_FAILED = "Failed to parse";
    private static final String ERROR_MESSAGE_INVALID = "is invalid";

    @Mock
    private PhoneNumberUtil phoneUtil;

    private PiiNormalizer piiNormalizer;

    @BeforeEach
    void setUp() {
        piiNormalizer = new PiiNormalizer(phoneUtil, DEFAULT_REGION_CODE);
    }

    @Nested
    @DisplayName("Email Normalization Tests")
    class EmailNormalizationTests {

        @Test
        @DisplayName("Should return lowercase trimmed email when given valid email")
        void shouldReturnLowercaseTrimmedEmail_whenGivenValidEmail() {
            // Given
            String expectedEmail = "test@example.com";

            // When
            String normalizedEmail = piiNormalizer.normalizeEmail(VALID_EMAIL);

            // Then
            assertThat(normalizedEmail).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("Should return lowercase email when given email with uppercase")
        void shouldReturnLowercaseEmail_whenGivenEmailWithUppercase() {
            // Given
            String expectedEmail = "test@example.com";

            // When
            String normalizedEmail = piiNormalizer.normalizeEmail(VALID_EMAIL_WITH_UPPERCASE);

            // Then
            assertThat(normalizedEmail).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("Should return trimmed email when given email with whitespace")
        void shouldReturnTrimmedEmail_whenGivenEmailWithWhitespace() {
            // Given
            String expectedEmail = "test@example.com";

            // When
            String normalizedEmail = piiNormalizer.normalizeEmail(VALID_EMAIL_WITH_WHITESPACE);

            // Then
            assertThat(normalizedEmail).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("Should normalize correctly when given email with special characters")
        void shouldNormalizeCorrectly_whenGivenEmailWithSpecialCharacters() {
            // Given
            String emailWithSpecialChars = "user+test_123@sub-domain.example.com";
            String expectedEmail = "user+test_123@sub-domain.example.com";

            // When
            String normalizedEmail = piiNormalizer.normalizeEmail(emailWithSpecialChars);

            // Then
            assertThat(normalizedEmail).isEqualTo(expectedEmail);
        }

        @Test
        @DisplayName("Should throw exception when given null email")
        void shouldThrowException_whenGivenNullEmail() {
            // Given
            String nullEmail = null;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(nullEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception when given empty email")
        void shouldThrowException_whenGivenEmptyEmail() {
            // Given
            String emptyEmail = "";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emptyEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception when given whitespace only email")
        void shouldThrowException_whenGivenWhitespaceOnlyEmail() {
            // Given
            String whitespaceEmail = "   ";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(whitespaceEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception when given email exceeding max length")
        void shouldThrowException_whenGivenEmailExceedingMaxLength() {
            // Given - Create email longer than 320 characters
            String localPart = "a".repeat(64);
            String domain = "b".repeat(250) + ".com";
            String tooLongEmail = localPart + "@" + domain;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(tooLongEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining("maximum length");
        }

        @Test
        @DisplayName("Should throw exception when given invalid email format")
        void shouldThrowException_whenGivenInvalidEmailFormat() {
            // Given
            String invalidEmail = "invalid-email-without-at";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(invalidEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given email without domain")
        void shouldThrowException_whenGivenEmailWithoutDomain() {
            // Given
            String emailWithoutDomain = "user@";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithoutDomain))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given email without local part")
        void shouldThrowException_whenGivenEmailWithoutLocalPart() {
            // Given
            String emailWithoutLocalPart = "@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithoutLocalPart))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given local part exceeding max length")
        void shouldThrowException_whenGivenLocalPartExceedingMaxLength() {
            // Given - Create local part longer than 64 characters
            String tooLongLocalPart = "a".repeat(65);
            String emailWithLongLocalPart = tooLongLocalPart + "@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithLongLocalPart))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining("Local part exceeds maximum length");
        }

        @Test
        @DisplayName("Should throw exception when given domain exceeding max length")
        void shouldThrowException_whenGivenDomainExceedingMaxLength() {
            // Given - Create domain longer than 255 characters
            String tooLongDomain = "a".repeat(250) + ".com";
            String emailWithLongDomain = "user@" + tooLongDomain;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithLongDomain))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining("Domain exceeds maximum length");
        }

        @Test
        @DisplayName("Should throw exception when given domain with consecutive dots")
        void shouldThrowException_whenGivenDomainWithConsecutiveDots() {
            // Given
            String emailWithConsecutiveDots = "user@example..com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithConsecutiveDots))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given domain starting with dot")
        void shouldThrowException_whenGivenDomainStartingWithDot() {
            // Given
            String emailWithDomainStartingWithDot = "user@.example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithDomainStartingWithDot))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given domain ending with dot")
        void shouldThrowException_whenGivenDomainEndingWithDot() {
            // Given
            String emailWithDomainEndingWithDot = "user@example.com.";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithDomainEndingWithDot))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given email with invalid characters")
        void shouldThrowException_whenGivenEmailWithInvalidCharacters() {
            // Given
            String emailWithInvalidChars = "user@exam ple.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithInvalidChars))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }

        @Test
        @DisplayName("Should throw exception when given email with short TLD")
        void shouldThrowException_whenGivenEmailWithShortTLD() {
            // Given
            String emailWithShortTLD = "user@example.c";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithShortTLD))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID_FORMAT);
        }
    }

    @Nested
    @DisplayName("Phone Number Normalization Tests")
    class PhoneNumberNormalizationTests {

        @Test
        @DisplayName("Should return national format when given valid phone number")
        void shouldReturnNationalFormat_whenGivenValidPhoneNumber() throws NumberParseException {
            // Given
            Phonenumber.PhoneNumber parsedNumber = createValidPhoneNumber();
            when(phoneUtil.parse(VALID_PHONE_NUMBER, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn(NATIONAL_FORMAT_PHONE);

            // When
            String normalizedPhone = piiNormalizer.normalizePhoneNumber(VALID_PHONE_NUMBER);

            // Then
            assertThat(normalizedPhone).isEqualTo(NATIONAL_FORMAT_PHONE);
            verify(phoneUtil).parse(VALID_PHONE_NUMBER, DEFAULT_REGION_CODE);
            verify(phoneUtil).isValidNumber(parsedNumber);
            verify(phoneUtil).format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        }

        @Test
        @DisplayName("Should normalize correctly when given phone number with whitespace")
        void shouldNormalizeCorrectly_whenGivenPhoneNumberWithWhitespace() throws NumberParseException {
            // Given
            String phoneWithWhitespace = "  +1234567890  ";
            Phonenumber.PhoneNumber parsedNumber = createValidPhoneNumber();

            // CRITICAL: Mock must match the ACTUAL parameter passed (original string, not trimmed)
            // The implementation passes the original phoneNumber to phoneUtil.parse()
            when(phoneUtil.parse(phoneWithWhitespace, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn(NATIONAL_FORMAT_PHONE);

            // When
            String normalizedPhone = piiNormalizer.normalizePhoneNumber(phoneWithWhitespace);

            // Then
            assertThat(normalizedPhone).isEqualTo(NATIONAL_FORMAT_PHONE);
            verify(phoneUtil).parse(phoneWithWhitespace, DEFAULT_REGION_CODE);
        }

        @Test
        @DisplayName("Should throw exception when given null phone number")
        void shouldThrowException_whenGivenNullPhoneNumber() {
            // Given
            String nullPhone = null;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(nullPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);

            // Note: No mock verification needed - validation fails before phoneUtil is called
        }

        @Test
        @DisplayName("Should throw exception when given empty phone number")
        void shouldThrowException_whenGivenEmptyPhoneNumber() {
            // Given
            String emptyPhone = "";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(emptyPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);

            // Note: No mock verification needed - validation fails before phoneUtil is called
        }

        @Test
        @DisplayName("Should throw exception when given whitespace only phone number")
        void shouldThrowException_whenGivenWhitespaceOnlyPhoneNumber() {
            // Given
            String whitespacePhone = "   ";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(whitespacePhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_NULL_OR_EMPTY);

            // Note: No mock verification needed - validation fails before phoneUtil is called
        }

        @Test
        @DisplayName("Should throw exception when given invalid phone number")
        void shouldThrowException_whenGivenInvalidPhoneNumber() throws NumberParseException {
            // Given
            String invalidPhone = "+9999999999999";
            Phonenumber.PhoneNumber parsedNumber = createInvalidPhoneNumber();

            when(phoneUtil.parse(invalidPhone, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(invalidPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_INVALID)
                    .hasMessageContaining(invalidPhone);

            // Verify parse and validate were called, but format was not (invalid number)
            verify(phoneUtil).parse(invalidPhone, DEFAULT_REGION_CODE);
            verify(phoneUtil).isValidNumber(parsedNumber);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given unparseable phone number")
        void shouldThrowException_whenGivenUnparseablePhoneNumber() throws NumberParseException {
            // Given
            String unparseablePhone = "not-a-phone-number";

            when(phoneUtil.parse(unparseablePhone, DEFAULT_REGION_CODE))
                    .thenThrow(new NumberParseException(
                            NumberParseException.ErrorType.NOT_A_NUMBER,
                            "Invalid phone number"));

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(unparseablePhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_PARSE_FAILED)
                    .hasMessageContaining(unparseablePhone)
                    .hasCauseInstanceOf(NumberParseException.class);

            // Verify parse was called, but validation and format were not (parse threw exception)
            verify(phoneUtil).parse(unparseablePhone, DEFAULT_REGION_CODE);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given phone number with invalid country code")
        void shouldThrowException_whenGivenPhoneNumberWithInvalidCountryCode()
                throws NumberParseException {
            // Given
            String phoneWithInvalidCountry = "+999123456789";

            when(phoneUtil.parse(phoneWithInvalidCountry, DEFAULT_REGION_CODE))
                    .thenThrow(new NumberParseException(
                            NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                            "Invalid country code"));

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(phoneWithInvalidCountry))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_PARSE_FAILED)
                    .hasCauseInstanceOf(NumberParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when given phone number too short")
        void shouldThrowException_whenGivenPhoneNumberTooShort() throws NumberParseException {
            // Given
            String tooShortPhone = "+1234";

            when(phoneUtil.parse(tooShortPhone, DEFAULT_REGION_CODE))
                    .thenThrow(new NumberParseException(
                            NumberParseException.ErrorType.TOO_SHORT_NSN,
                            "Phone number too short"));

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(tooShortPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_PARSE_FAILED)
                    .hasCauseInstanceOf(NumberParseException.class);
        }

        @Test
        @DisplayName("Should throw exception when given phone number too long")
        void shouldThrowException_whenGivenPhoneNumberTooLong() throws NumberParseException {
            // Given
            String tooLongPhone = "+123456789012345678901234567890";

            when(phoneUtil.parse(tooLongPhone, DEFAULT_REGION_CODE))
                    .thenThrow(new NumberParseException(
                            NumberParseException.ErrorType.TOO_LONG,
                            "Phone number too long"));

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(tooLongPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessageContaining(ERROR_MESSAGE_PARSE_FAILED)
                    .hasCauseInstanceOf(NumberParseException.class);
        }

        /**
         * Helper method to create a valid phone number for testing.
         *
         * @return a valid phone number object
         */
        private Phonenumber.PhoneNumber createValidPhoneNumber() {
            Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
            phoneNumber.setCountryCode(1);
            phoneNumber.setNationalNumber(1234567890L);
            return phoneNumber;
        }

        /**
         * Helper method to create an invalid phone number for testing.
         *
         * @return an invalid phone number object
         */
        private Phonenumber.PhoneNumber createInvalidPhoneNumber() {
            Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
            phoneNumber.setCountryCode(999);
            phoneNumber.setNationalNumber(9999999999L);
            return phoneNumber;
        }
    }
}