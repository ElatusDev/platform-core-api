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

/**
 * Collaborator-specific data generation. Delegates shared person fields
 * to {@link PersonDataGenerator}.
 */
@Component
@RequiredArgsConstructor
public class CollaboratorDataGenerator {

    private final PersonDataGenerator personData;

    private static final List<String> SKILL_SETS = Arrays.asList(
            "Mathematics, Physics",
            "Chemistry, Biology",
            "Spanish Literature, Grammar",
            "English, French",
            "Music, Art",
            "Physical Education, Dance",
            "Programming, Robotics",
            "History, Social Studies",
            "Accounting, Finance",
            "Yoga, Meditation"
    );

    private static final List<String> ROLES = Arrays.asList(
            "COLLABORATOR", "INSTRUCTOR", "SPECIALIST"
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

    public String username(String firstName, String lastName) {
        return personData.username(firstName, lastName);
    }

    public String password() {
        return personData.password();
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
        return personData.birthdate(25, 60);
    }

    public LocalDate entryDate() {
        return personData.entryDate();
    }

    public String skills() {
        return SKILL_SETS.get(personData.random().nextInt(SKILL_SETS.size()));
    }

    public String role() {
        return ROLES.get(personData.random().nextInt(ROLES.size()));
    }

    public JsonNullable<byte[]> profilePicture() {
        return personData.profilePicture();
    }
}
