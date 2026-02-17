/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.users;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating AdultStudentCreationRequestDTO instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class AdultStudentFactory implements DataFactory<AdultStudentCreationRequestDTO> {

    private final AdultStudentDataGenerator generator;

    @Override
    public List<AdultStudentCreationRequestDTO> generate(int count) {
        List<AdultStudentCreationRequestDTO> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            students.add(createAdultStudent());
        }
        return students;
    }

    private AdultStudentCreationRequestDTO createAdultStudent() {
        String firstName = generator.firstName();
        String lastName = generator.lastName();

        AdultStudentCreationRequestDTO dto = new AdultStudentCreationRequestDTO(
                generator.birthdate(),
                firstName,
                lastName,
                generator.email(firstName, lastName),
                generator.phoneNumber(),
                generator.address(),
                generator.zipCode(),
                generator.provider(),
                generator.token()
        );
        dto.setProfilePicture(generator.profilePicture());
        return dto;
    }
}
