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
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating TutorCreationRequestDTO instances with fake data.
 * All generated tutors include optional CustomerAuth credentials
 * so that the mock data exercises the full authentication path.
 */
@Component
@RequiredArgsConstructor
public class TutorFactory implements DataFactory<TutorCreationRequestDTO> {

    private final TutorDataGenerator generator;

    @Override
    public List<TutorCreationRequestDTO> generate(int count) {
        List<TutorCreationRequestDTO> tutors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tutors.add(createTutor());
        }
        return tutors;
    }

    private TutorCreationRequestDTO createTutor() {
        String firstName = generator.firstName();
        String lastName = generator.lastName();

        TutorCreationRequestDTO dto = new TutorCreationRequestDTO(
                generator.birthdate(),
                firstName,
                lastName,
                generator.email(firstName, lastName),
                generator.phoneNumber(),
                generator.address(),
                generator.zipCode()
        );
        dto.setProvider(JsonNullable.of(generator.provider()));
        dto.setToken(JsonNullable.of(generator.token()));
        return dto;
    }
}
