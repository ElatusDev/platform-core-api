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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normalizes Personally Identifiable Information (PII) data to ensure consistency
 * across the application. Handles email addresses and phone numbers according to
 * international standards.
 *
 * <p>This component provides:
 * <ul>
 *   <li>Email normalization (lowercase, trimmed, validated)</li>
 *   <li>Phone number parsing, validation, and formatting using libphonenumber</li>
 * </ul>
 *
 * <p><b>Security Note:</b> Email validation uses a safe regex pattern with possessive
 * quantifiers to prevent ReDoS (Regular Expression Denial of Service) attacks.
 *
 * @author ElatusDev
 * @version 1.1
 * @since 1.0
 */
@PropertySource("classpath:utilities.properties")
@Component
public class PiiNormalizer {

    // Error messages as constants
    private static final String ERROR_EMAIL_NULL_OR_EMPTY = "Email address cannot be null or empty";
    private static final String ERROR_EMAIL_INVALID_FORMAT = "Email address format is invalid: %s";
    private static final String ERROR_PHONE_NULL_OR_EMPTY = "Phone number cannot be null or empty";
    private static final String ERROR_PHONE_INVALID = "Phone number is invalid: %s";
    private static final String ERROR_PHONE_PARSE_FAILED = "Failed to parse phone number: %s";

    // RFC 5321 email length limits
    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_LENGTH = 255;

    /**
     * Safe email pattern using possessive quantifiers to prevent catastrophic backtracking.
     *
     * <p>Pattern breakdown:
     * <ul>
     *   <li>[a-zA-Z0-9_+&*-]++ : Local part characters (possessive to prevent backtracking)</li>
     *   <li>(?:\\.[a-zA-Z0-9_+&*-]++)* : Optional dots with more characters (possessive)</li>
     *   <li>@ : Required at symbol</li>
     *   <li>(?:[a-zA-Z0-9-]++\\.)* : Domain parts with dots (possessive)</li>
     *   <li>[a-zA-Z]{2,}+ : TLD with at least 2 characters (possessive)</li>
     * </ul>
     *
     * <p><b>Security:</b> Possessive quantifiers (++) prevent the regex engine from
     * backtracking, making this pattern immune to ReDoS attacks even with malicious input.
     *
     * @see <a href="https://www.regular-expressions.info/possessive.html">Possessive Quantifiers</a>
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]++(?:\\.[a-zA-Z0-9_+&*-]++)*+@(?:[a-zA-Z0-9-]++\\.)*+[a-zA-Z]{2,}+$"
    );

    private final PhoneNumberUtil phoneUtil;
    private final String defaultRegionCode;

    /**
     * Constructs a new PiiNormalizer with the specified phone number utility and default region.
     *
     * @param phoneUtil         the phone number utility for parsing and validation
     * @param defaultRegionCode the default region code for phone number parsing (e.g., "US", "MX")
     */
    public PiiNormalizer(
            PhoneNumberUtil phoneUtil,
            @Value("${app.phone.default-region}") String defaultRegionCode) {
        this.phoneUtil = phoneUtil;
        this.defaultRegionCode = defaultRegionCode;
    }

    /**
     * Normalizes an email address by trimming whitespace, converting to lowercase,
     * and validating the format.
     *
     * <p>Validation includes:
     * <ul>
     *   <li>Length checks according to RFC 5321</li>
     *   <li>Basic format validation using safe regex</li>
     *   <li>Domain structure validation</li>
     * </ul>
     *
     * @param email the email address to normalize
     * @return the normalized email address in lowercase
     * @throws ErrorNormalizationException if the email is null, empty, or has an invalid format
     */
    public String normalizeEmail(String email) {
        validateNotNullOrEmpty(email, ERROR_EMAIL_NULL_OR_EMPTY);

        String trimmedEmail = email.trim();

        // Length validation (prevents ReDoS and ensures RFC compliance)
        if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT, "Email exceeds maximum length of " + MAX_EMAIL_LENGTH));
        }

        // Format validation with safe regex
        if (!isValidEmailFormat(trimmedEmail)) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT, trimmedEmail));
        }

        // Additional structural validation
        validateEmailStructure(trimmedEmail);

        return trimmedEmail.toLowerCase();
    }

    /**
     * Normalizes a phone number by parsing it according to the default region,
     * validating it, and formatting it in national format.
     *
     * @param phoneNumber the phone number string to normalize
     * @return the normalized phone number in national format
     * @throws ErrorNormalizationException if the phone number is null, empty, invalid, or cannot be parsed
     */
    public String normalizePhoneNumber(String phoneNumber) {
        validateNotNullOrEmpty(phoneNumber, ERROR_PHONE_NULL_OR_EMPTY);

        try {
            Phonenumber.PhoneNumber parsedNumber = parsePhoneNumber(phoneNumber);
            validatePhoneNumber(parsedNumber, phoneNumber);
            return formatPhoneNumber(parsedNumber);
        } catch (NumberParseException e) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_PHONE_PARSE_FAILED, phoneNumber), e);
        }
    }

    /**
     * Validates that a string is not null or empty after trimming.
     *
     * @param value        the string to validate
     * @param errorMessage the error message to use if validation fails
     * @throws ErrorNormalizationException if the string is null or empty
     */
    private void validateNotNullOrEmpty(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new ErrorNormalizationException(errorMessage);
        }
    }

    /**
     * Validates an email address format using a safe regex pattern with possessive quantifiers.
     *
     * @param email the email address to validate
     * @return true if the email format is valid, false otherwise
     */
    private boolean isValidEmailFormat(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Performs additional structural validation on the email address.
     * This method validates RFC 5321 constraints that regex cannot efficiently check.
     *
     * @param email the email address to validate
     * @throws ErrorNormalizationException if the email structure is invalid
     */
    private void validateEmailStructure(String email) {
        int atIndex = email.indexOf('@');

        // This should never happen if regex passed, but defensive check
        if (atIndex == -1) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT, email));
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        // Validate length constraints (RFC 5321)
        if (localPart.length() > MAX_LOCAL_PART_LENGTH) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT,
                            "Local part exceeds maximum length of " + MAX_LOCAL_PART_LENGTH));
        }

        if (domain.length() > MAX_DOMAIN_LENGTH) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT,
                            "Domain exceeds maximum length of " + MAX_DOMAIN_LENGTH));
        }

        // Validate domain doesn't have consecutive dots or start/end with dot
        if (domain.contains("..") || domain.startsWith(".") || domain.endsWith(".")) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_EMAIL_INVALID_FORMAT, email));
        }
    }

    /**
     * Parses a phone number string using the configured default region.
     *
     * @param phoneNumber the phone number string to parse
     * @return the parsed phone number object
     * @throws NumberParseException if the phone number cannot be parsed
     */
    private Phonenumber.PhoneNumber parsePhoneNumber(String phoneNumber) throws NumberParseException {
        return phoneUtil.parse(phoneNumber, defaultRegionCode);
    }

    /**
     * Validates a parsed phone number.
     *
     * @param parsedNumber    the parsed phone number to validate
     * @param originalNumber  the original phone number string for error reporting
     * @throws ErrorNormalizationException if the phone number is not valid
     */
    private void validatePhoneNumber(Phonenumber.PhoneNumber parsedNumber, String originalNumber) {
        if (!phoneUtil.isValidNumber(parsedNumber)) {
            throw new ErrorNormalizationException(
                    String.format(ERROR_PHONE_INVALID, originalNumber));
        }
    }

    /**
     * Formats a phone number in national format.
     *
     * @param phoneNumber the phone number to format
     * @return the formatted phone number string
     */
    private String formatPhoneNumber(Phonenumber.PhoneNumber phoneNumber) {
        return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    }
}