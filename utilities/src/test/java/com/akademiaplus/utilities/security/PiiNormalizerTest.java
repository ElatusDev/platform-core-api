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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_EMAIL_NULL_OR_EMPTY;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_EMAIL_INVALID_FORMAT;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_EMAIL_EXCEEDS_MAX_LENGTH;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_LOCAL_PART_EXCEEDS_MAX_LENGTH;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_DOMAIN_EXCEEDS_MAX_LENGTH;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_PHONE_NULL_OR_EMPTY;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_PHONE_INVALID;
import static com.akademiaplus.utilities.security.PiiNormalizer.ERROR_PHONE_PARSE_FAILED;

/**
 * Comprehensive test suite for {@link PiiNormalizer}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Email normalization with RFC 5321 compliance</li>
 *   <li>Phone number parsing, validation, and formatting</li>
 *   <li>Security validation (ReDoS prevention)</li>
 *   <li>Edge cases and error conditions</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PiiNormalizerTest {

    // Shared test constants
    private static final String DEFAULT_REGION_CODE = "US";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PHONE = "+1234567890";
    private static final String FORMATTED_PHONE_NATIONAL = "(123) 456-7890";

    // RFC 5321 length constants (matching implementation)
    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_LENGTH = 255;

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
        @DisplayName("Should normalize email to lowercase when given valid uppercase email")
        void shouldNormalizeEmailToLowercase_whenGivenValidUppercaseEmail() {
            // Given
            String upperCaseEmail = "Test@Example.COM";

            // When
            String result = piiNormalizer.normalizeEmail(upperCaseEmail);

            // Then
            assertThat(result).isEqualTo("test@example.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should trim and normalize email when given email with whitespace")
        void shouldTrimAndNormalizeEmail_whenGivenEmailWithWhitespace() {
            // Given
            String emailWithWhitespace = "  test@example.com  ";

            // When
            String result = piiNormalizer.normalizeEmail(emailWithWhitespace);

            // Then
            assertThat(result).isEqualTo("test@example.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should return normalized email when given valid email with numbers")
        void shouldReturnNormalizedEmail_whenGivenValidEmailWithNumbers() {
            // Given
            String emailWithNumbers = "user123@test456.com";

            // When
            String result = piiNormalizer.normalizeEmail(emailWithNumbers);

            // Then
            assertThat(result).isEqualTo("user123@test456.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize email when given valid email with special characters")
        void shouldNormalizeEmail_whenGivenValidEmailWithSpecialCharacters() {
            // Given
            String emailWithSpecialChars = "user+test_123@example.com";

            // When
            String result = piiNormalizer.normalizeEmail(emailWithSpecialChars);

            // Then
            assertThat(result).isEqualTo("user+test_123@example.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize email when given valid email with subdomain")
        void shouldNormalizeEmail_whenGivenValidEmailWithSubdomain() {
            // Given
            String emailWithSubdomain = "user@mail.example.com";

            // When
            String result = piiNormalizer.normalizeEmail(emailWithSubdomain);

            // Then
            assertThat(result).isEqualTo("user@mail.example.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize email when given email with dots in local part")
        void shouldNormalizeEmail_whenGivenEmailWithDotsInLocalPart() {
            // Given
            String emailWithDots = "first.last@example.com";

            // When
            String result = piiNormalizer.normalizeEmail(emailWithDots);

            // Then
            assertThat(result).isEqualTo("first.last@example.com");
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given null email")
        void shouldThrowException_whenGivenNullEmail() {
            // Given
            String nullEmail = null;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(nullEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_EMAIL_NULL_OR_EMPTY);
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given empty email")
        void shouldThrowException_whenGivenEmptyEmail() {
            // Given
            String emptyEmail = "";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emptyEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_EMAIL_NULL_OR_EMPTY);
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given whitespace-only email")
        void shouldThrowException_whenGivenWhitespaceOnlyEmail() {
            // Given
            String whitespaceEmail = "   ";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(whitespaceEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_EMAIL_NULL_OR_EMPTY);
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email exceeding maximum length")
        void shouldThrowException_whenGivenEmailExceedingMaximumLength() {
            // Given
            String localPart = "a".repeat(64);
            String domain = "b".repeat(252) + ".com";
            String tooLongEmail = localPart + "@" + domain;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(tooLongEmail))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_EXCEEDS_MAX_LENGTH, MAX_EMAIL_LENGTH));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email without at symbol")
        void shouldThrowException_whenGivenEmailWithoutAtSymbol() {
            // Given
            String emailWithoutAt = "testexample.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithoutAt))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithoutAt));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with multiple at symbols")
        void shouldThrowException_whenGivenEmailWithMultipleAtSymbols() {
            // Given
            String emailWithMultipleAt = "test@@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithMultipleAt))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithMultipleAt));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with invalid characters")
        void shouldThrowException_whenGivenEmailWithInvalidCharacters() {
            // Given
            String emailWithInvalidChars = "test#$%@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithInvalidChars))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithInvalidChars));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with space in local part")
        void shouldThrowException_whenGivenEmailWithSpaceInLocalPart() {
            // Given
            String emailWithSpace = "test user@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithSpace))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithSpace));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with local part exceeding maximum length")
        void shouldThrowException_whenGivenEmailWithLocalPartExceedingMaximumLength() {
            // Given
            String localPart = "a".repeat(MAX_LOCAL_PART_LENGTH + 1);
            String emailWithLongLocalPart = localPart + "@example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithLongLocalPart))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_LOCAL_PART_EXCEEDS_MAX_LENGTH, MAX_LOCAL_PART_LENGTH));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with domain exceeding maximum length")
        void shouldThrowException_whenGivenEmailWithDomainExceedingMaximumLength() {
            // Given
            String domain = "a".repeat(MAX_DOMAIN_LENGTH + 1);
            String emailWithLongDomain = "test@" + domain;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithLongDomain))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_DOMAIN_EXCEEDS_MAX_LENGTH, MAX_DOMAIN_LENGTH));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with consecutive dots in domain")
        void shouldThrowException_whenGivenEmailWithConsecutiveDotsInDomain() {
            // Given
            String emailWithConsecutiveDots = "test@example..com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithConsecutiveDots))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithConsecutiveDots));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with domain starting with dot")
        void shouldThrowException_whenGivenEmailWithDomainStartingWithDot() {
            // Given
            String emailWithDotStart = "test@.example.com";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithDotStart))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithDotStart));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with domain ending with dot")
        void shouldThrowException_whenGivenEmailWithDomainEndingWithDot() {
            // Given
            String emailWithDotEnd = "test@example.com.";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithDotEnd))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithDotEnd));
            verifyNoInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given email with single character TLD")
        void shouldThrowException_whenGivenEmailWithSingleCharacterTld() {
            // Given
            String emailWithSingleCharTld = "test@example.c";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizeEmail(emailWithSingleCharTld))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_EMAIL_INVALID_FORMAT, emailWithSingleCharTld));
            verifyNoInteractions(phoneUtil);
        }
    }

    @Nested
    @DisplayName("Phone Number Normalization Tests")
    class PhoneNumberNormalizationTests {

        @Test
        @DisplayName("Should normalize phone number when given valid phone number")
        void shouldNormalizePhoneNumber_whenGivenValidPhoneNumber() throws NumberParseException {
            // Given
            String validPhone = "+12025551234";
            Phonenumber.PhoneNumber parsedNumber = createPhoneNumber(1, 2025551234L);

            when(phoneUtil.parse(validPhone, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn("(202) 555-1234");

            // When
            String result = piiNormalizer.normalizePhoneNumber(validPhone);

            // Then
            assertThat(result).isEqualTo("(202) 555-1234");
            verify(phoneUtil, times(1)).parse(validPhone, DEFAULT_REGION_CODE);
            verify(phoneUtil, times(1)).isValidNumber(parsedNumber);
            verify(phoneUtil, times(1)).format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize phone number when given phone with whitespace")
        void shouldNormalizePhoneNumber_whenGivenPhoneWithWhitespace() throws NumberParseException {
            // Given
            String phoneWithWhitespace = "  +12025551234  ";
            String trimmedPhone = "+12025551234";
            Phonenumber.PhoneNumber parsedNumber = createPhoneNumber(1, 2025551234L);

            when(phoneUtil.parse(trimmedPhone, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn("(202) 555-1234");

            // When
            String result = piiNormalizer.normalizePhoneNumber(phoneWithWhitespace);

            // Then
            assertThat(result).isEqualTo("(202) 555-1234");
            verify(phoneUtil, times(1)).parse(trimmedPhone, DEFAULT_REGION_CODE);
            verify(phoneUtil, times(1)).isValidNumber(parsedNumber);
            verify(phoneUtil, times(1)).format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize phone number when given phone without country code")
        void shouldNormalizePhoneNumber_whenGivenPhoneWithoutCountryCode() throws NumberParseException {
            // Given
            String phoneWithoutCountryCode = "2025551234";
            Phonenumber.PhoneNumber parsedNumber = createPhoneNumber(1, 2025551234L);

            when(phoneUtil.parse(phoneWithoutCountryCode, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn("(202) 555-1234");

            // When
            String result = piiNormalizer.normalizePhoneNumber(phoneWithoutCountryCode);

            // Then
            assertThat(result).isEqualTo("(202) 555-1234");
            verify(phoneUtil, times(1)).parse(phoneWithoutCountryCode, DEFAULT_REGION_CODE);
            verify(phoneUtil, times(1)).isValidNumber(parsedNumber);
            verify(phoneUtil, times(1)).format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should normalize phone number when given phone with dashes")
        void shouldNormalizePhoneNumber_whenGivenPhoneWithDashes() throws NumberParseException {
            // Given
            String phoneWithDashes = "202-555-1234";
            Phonenumber.PhoneNumber parsedNumber = createPhoneNumber(1, 2025551234L);

            when(phoneUtil.parse(phoneWithDashes, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(true);
            when(phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                    .thenReturn("(202) 555-1234");

            // When
            String result = piiNormalizer.normalizePhoneNumber(phoneWithDashes);

            // Then
            assertThat(result).isEqualTo("(202) 555-1234");
            verify(phoneUtil, times(1)).parse(phoneWithDashes, DEFAULT_REGION_CODE);
            verify(phoneUtil, times(1)).isValidNumber(parsedNumber);
            verify(phoneUtil, times(1)).format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given null phone number")
        void shouldThrowException_whenGivenNullPhoneNumber() {
            // Given
            String nullPhone = null;

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(nullPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_PHONE_NULL_OR_EMPTY);

            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given empty phone number")
        void shouldThrowException_whenGivenEmptyPhoneNumber() {
            // Given
            String emptyPhone = "";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(emptyPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_PHONE_NULL_OR_EMPTY);

            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when given whitespace-only phone number")
        void shouldThrowException_whenGivenWhitespaceOnlyPhoneNumber() {
            // Given
            String whitespacePhone = "   ";

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(whitespacePhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(ERROR_PHONE_NULL_OR_EMPTY);

            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when phone parsing fails")
        void shouldThrowException_whenPhoneParsingFails() throws NumberParseException {
            // Given
            String invalidPhone = "invalid";
            NumberParseException parseException = new NumberParseException(
                    NumberParseException.ErrorType.NOT_A_NUMBER,
                    "The string supplied did not seem to be a phone number"
            );

            when(phoneUtil.parse(invalidPhone, DEFAULT_REGION_CODE)).thenThrow(parseException);

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(invalidPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_PHONE_PARSE_FAILED, invalidPhone))
                    .hasCause(parseException);

            verify(phoneUtil, times(1)).parse(invalidPhone, DEFAULT_REGION_CODE);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when parsed phone number is invalid")
        void shouldThrowException_whenParsedPhoneNumberIsInvalid() throws NumberParseException {
            // Given
            String parseableButInvalidPhone = "123";
            Phonenumber.PhoneNumber parsedNumber = createPhoneNumber(1, 123L);

            when(phoneUtil.parse(parseableButInvalidPhone, DEFAULT_REGION_CODE)).thenReturn(parsedNumber);
            when(phoneUtil.isValidNumber(parsedNumber)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(parseableButInvalidPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_PHONE_INVALID, parseableButInvalidPhone));

            verify(phoneUtil, times(1)).parse(parseableButInvalidPhone, DEFAULT_REGION_CODE);
            verify(phoneUtil, times(1)).isValidNumber(parsedNumber);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when phone number too short")
        void shouldThrowException_whenPhoneNumberTooShort() throws NumberParseException {
            // Given
            String tooShortPhone = "12";
            NumberParseException parseException = new NumberParseException(
                    NumberParseException.ErrorType.TOO_SHORT_NSN,
                    "The string supplied is too short to be a phone number"
            );

            when(phoneUtil.parse(tooShortPhone, DEFAULT_REGION_CODE)).thenThrow(parseException);

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(tooShortPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_PHONE_PARSE_FAILED, tooShortPhone))
                    .hasCause(parseException);

            verify(phoneUtil, times(1)).parse(tooShortPhone, DEFAULT_REGION_CODE);
            verifyNoMoreInteractions(phoneUtil);
        }

        @Test
        @DisplayName("Should throw exception when phone number too long")
        void shouldThrowException_whenPhoneNumberTooLong() throws NumberParseException {
            // Given
            String tooLongPhone = "12345678901234567890";
            NumberParseException parseException = new NumberParseException(
                    NumberParseException.ErrorType.TOO_LONG,
                    "The string supplied is too long to be a phone number"
            );

            when(phoneUtil.parse(tooLongPhone, DEFAULT_REGION_CODE)).thenThrow(parseException);

            // When & Then
            assertThatThrownBy(() -> piiNormalizer.normalizePhoneNumber(tooLongPhone))
                    .isInstanceOf(ErrorNormalizationException.class)
                    .hasMessage(String.format(ERROR_PHONE_PARSE_FAILED, tooLongPhone))
                    .hasCause(parseException);

            verify(phoneUtil, times(1)).parse(tooLongPhone, DEFAULT_REGION_CODE);
            verifyNoMoreInteractions(phoneUtil);
        }

        /**
         * Helper method to create a PhoneNumber object for testing.
         *
         * @param countryCode  the country code
         * @param nationalNumber the national number
         * @return a PhoneNumber object
         */
        private Phonenumber.PhoneNumber createPhoneNumber(int countryCode, long nationalNumber) {
            Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
            phoneNumber.setCountryCode(countryCode);
            phoneNumber.setNationalNumber(nationalNumber);
            return phoneNumber;
        }
    }
}