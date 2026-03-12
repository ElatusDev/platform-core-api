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
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating CollaboratorCreationRequestDTO instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class CollaboratorFactory implements DataFactory<CollaboratorCreationRequestDTO> {

    private final CollaboratorDataGenerator generator;

    @Override
    public List<CollaboratorCreationRequestDTO> generate(int count) {
        List<CollaboratorCreationRequestDTO> collaborators = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            collaborators.add(createCollaborator());
        }
        return collaborators;
    }

    private CollaboratorCreationRequestDTO createCollaborator() {
        String firstName = generator.firstName();
        String lastName = generator.lastName();

        CollaboratorCreationRequestDTO dto = new CollaboratorCreationRequestDTO(
                generator.skills(),
                generator.birthdate(),
                generator.entryDate(),
                firstName,
                lastName,
                generator.email(firstName, lastName),
                generator.phoneNumber(),
                generator.address(),
                generator.zipCode(),
                generator.username(firstName, lastName),
                generator.password(),
                generator.role()
        );
        dto.setProfilePicture(generator.profilePicture());
        return dto;
    }
}
