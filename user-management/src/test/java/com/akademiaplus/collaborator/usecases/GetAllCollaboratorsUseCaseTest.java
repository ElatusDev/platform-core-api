/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllCollaboratorsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllCollaboratorsUseCaseTest {

    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllCollaboratorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllCollaboratorsUseCase(collaboratorRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no collaborators exist")
        void shouldReturnEmptyList_whenNoCollaboratorsExist() {
            // Given
            when(collaboratorRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetCollaboratorResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(collaboratorRepository).findAll();
            verifyNoMoreInteractions(collaboratorRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when collaborators exist")
        void shouldReturnMappedDtos_whenCollaboratorsExist() {
            // Given
            PersonPIIDataModel personPII1 = new PersonPIIDataModel();
            PersonPIIDataModel personPII2 = new PersonPIIDataModel();
            CollaboratorDataModel collaborator1 = new CollaboratorDataModel();
            collaborator1.setPersonPII(personPII1);
            CollaboratorDataModel collaborator2 = new CollaboratorDataModel();
            collaborator2.setPersonPII(personPII2);
            GetCollaboratorResponseDTO dto1 = new GetCollaboratorResponseDTO();
            GetCollaboratorResponseDTO dto2 = new GetCollaboratorResponseDTO();

            when(collaboratorRepository.findAll()).thenReturn(List.of(collaborator1, collaborator2));
            when(modelMapper.map(collaborator1, GetCollaboratorResponseDTO.class)).thenReturn(dto1);
            doNothing().when(modelMapper).map(personPII1, dto1);
            when(modelMapper.map(collaborator2, GetCollaboratorResponseDTO.class)).thenReturn(dto2);
            doNothing().when(modelMapper).map(personPII2, dto2);

            // When
            List<GetCollaboratorResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(collaboratorRepository).findAll();
            verify(modelMapper).map(collaborator1, GetCollaboratorResponseDTO.class);
            verify(modelMapper).map(personPII1, dto1);
            verify(modelMapper).map(collaborator2, GetCollaboratorResponseDTO.class);
            verify(modelMapper).map(personPII2, dto2);
            verifyNoMoreInteractions(collaboratorRepository, modelMapper);
        }
    }
}
