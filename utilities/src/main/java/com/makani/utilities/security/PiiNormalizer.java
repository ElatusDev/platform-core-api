package com.makani.utilities.security;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.makani.utilities.exceptions.ErrorNormalizationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;


@PropertySource("classpath:utilities.properties")
@Component
public class PiiNormalizer {
    private final PhoneNumberUtil phoneUtil;
    private final String defaultRegionCode;

    public PiiNormalizer(PhoneNumberUtil phoneUtil, @Value("${app.phone.default-region}")
    String defaultRegionCode) {
        this.phoneUtil = phoneUtil;
        this.defaultRegionCode = defaultRegionCode;
    }

    public String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ErrorNormalizationException("email cannot be null");
        }
        return email.trim().toLowerCase(); // Basic normalization
    }

    public String normalizePhoneNumber(String phoneNumberString) {
        if (phoneNumberString == null || phoneNumberString.trim().isEmpty()) {
            throw new ErrorNormalizationException("phone number is empty");
        }

        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberString, defaultRegionCode);

            if (!phoneUtil.isValidNumber(phoneNumber)) {
                throw new NoSuchElementException("Warning: Invalid phone number detected: " + phoneNumberString);
            }

            return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (Exception e) {
            throw new ErrorNormalizationException(e);
        }
    }
}
