/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.users;

import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Tutor-specific data generation. Delegates shared person fields
 * to {@link PersonDataGenerator}. Tutors use optional CustomerAuth
 * (provider + token) — not all tutors self-authenticate.
 */
@Component
@RequiredArgsConstructor
public class TutorDataGenerator {

    private final PersonDataGenerator personData;

    private static final List<String> AUTH_PROVIDERS = Arrays.asList(
            "GOOGLE", "FACEBOOK", "APPLE"
    );

    public String firstName() {
        return personData.firstName();
    }

    public String lastName() {
        return personData.lastName();
    }

    public String email(String firstName, String lastName) {
        return personData.email(firstName, lastName);
    }

    public String phoneNumber() {
        return personData.phoneNumber();
    }

    public String address() {
        return personData.address();
    }

    public String zipCode() {
        return personData.zipCode();
    }

    public LocalDate birthdate() {
        return personData.birthdate(30, 65);
    }

    public String provider() {
        return AUTH_PROVIDERS.get(personData.random().nextInt(AUTH_PROVIDERS.size()));
    }

    public String token() {
        return "oauth_" + UUID.randomUUID().toString().replace("-", "");
    }
}
