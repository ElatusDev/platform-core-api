/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.exception.CollaboratorNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetCollaboratorByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetCollaboratorByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COLLABORATOR_ID = 100L;

    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetCollaboratorByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCollaboratorByIdUseCase(collaboratorRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when collaborator is found")
        void shouldReturnMappedDto_whenCollaboratorFound() {
            // Given
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            collaborator.setPersonPII(personPII);
            GetCollaboratorResponseDTO expectedDto = new GetCollaboratorResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID)))
                    .thenReturn(Optional.of(collaborator));
            when(modelMapper.map(collaborator, GetCollaboratorResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetCollaboratorResponseDTO result = useCase.get(COLLABORATOR_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(collaboratorRepository).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID));
            verify(modelMapper).map(collaborator, GetCollaboratorResponseDTO.class);
            verify(modelMapper).map(personPII, expectedDto);
            verifyNoMoreInteractions(tenantContextHolder, collaboratorRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw CollaboratorNotFoundException when collaborator not found")
        void shouldThrowCollaboratorNotFoundException_whenCollaboratorNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(COLLABORATOR_ID))
                    .isInstanceOf(CollaboratorNotFoundException.class)
                    .hasMessage(String.valueOf(COLLABORATOR_ID));
            verify(tenantContextHolder).getTenantId();
            verify(collaboratorRepository).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID));
            verifyNoMoreInteractions(tenantContextHolder, collaboratorRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(COLLABORATOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetCollaboratorByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, collaboratorRepository, modelMapper);
        }
    }
}
