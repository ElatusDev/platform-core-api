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
import lombok.Setter;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating MinorStudentCreationRequestDTO instances with fake data.
 * <p>
 * Requires {@link #setAvailableTutorIds(List)} to be called before
 * {@link #generate(int)}, so that each minor student is assigned
 * to a valid, previously-persisted tutor. The orchestrator is
 * responsible for loading tutors first and providing their IDs.
 */
@Component
@RequiredArgsConstructor
public class MinorStudentFactory implements DataFactory<MinorStudentCreationRequestDTO> {

    private final MinorStudentDataGenerator generator;

    @Setter
    private List<Long> availableTutorIds = List.of();

    @Override
    public List<MinorStudentCreationRequestDTO> generate(int count) {
        if (availableTutorIds.isEmpty()) {
            throw new IllegalStateException("availableTutorIds must be set before generating minor students");
        }
        List<MinorStudentCreationRequestDTO> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long tutorId = availableTutorIds.get(i % availableTutorIds.size());
            students.add(createMinorStudent(tutorId));
        }
        return students;
    }

    private MinorStudentCreationRequestDTO createMinorStudent(Long tutorId) {
        String firstName = generator.firstName();
        String lastName = generator.lastName();

        MinorStudentCreationRequestDTO dto = new MinorStudentCreationRequestDTO(
                generator.birthdate(),
                tutorId,
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
