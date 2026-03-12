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
 * Employee-specific data generation. Delegates shared person fields
 * to {@link PersonDataGenerator}.
 */
@Component
@RequiredArgsConstructor
public class EmployeeDataGenerator {

    private final PersonDataGenerator personData;

    private static final List<String> EMPLOYEE_TYPES = Arrays.asList(
            "INSTRUCTOR", "ADMINISTRATOR", "COORDINATOR", "MANAGER", "ASSISTANT"
    );

    private static final List<String> ROLES = Arrays.asList(
            "EMPLOYEE", "ADMIN", "SUPERVISOR", "MANAGER", "USER"
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
        return personData.birthdate(22, 65);
    }

    public LocalDate entryDate() {
        return personData.entryDate();
    }

    public String employeeType() {
        return EMPLOYEE_TYPES.get(personData.random().nextInt(EMPLOYEE_TYPES.size()));
    }

    public String role() {
        return ROLES.get(personData.random().nextInt(ROLES.size()));
    }

    public JsonNullable<byte[]> profilePicture() {
        return personData.profilePicture();
    }
}
