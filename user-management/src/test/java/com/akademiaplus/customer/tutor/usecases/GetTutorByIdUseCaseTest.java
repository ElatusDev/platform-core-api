/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
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

@DisplayName("GetTutorByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetTutorByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TUTOR_ID = 100L;

    @Mock private TutorRepository tutorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetTutorByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTutorByIdUseCase(tutorRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when tutor is found")
        void shouldReturnMappedDto_whenTutorFound() {
            // Given
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            TutorDataModel tutor = new TutorDataModel();
            tutor.setPersonPII(personPII);
            GetTutorResponseDTO expectedDto = new GetTutorResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID)))
                    .thenReturn(Optional.of(tutor));
            when(modelMapper.map(tutor, GetTutorResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetTutorResponseDTO result = useCase.get(TUTOR_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(tutorRepository, times(1)).findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            verify(modelMapper, times(1)).map(tutor, GetTutorResponseDTO.class);
            verify(modelMapper, times(1)).map(personPII, expectedDto);
            verifyNoMoreInteractions(tenantContextHolder, tutorRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tutor not found")
        void shouldThrowEntityNotFoundException_whenTutorNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(TUTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TUTOR, TUTOR_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                    });
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(tutorRepository, times(1)).findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, tutorRepository);
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
            assertThatThrownBy(() -> useCase.get(TUTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetTutorByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder);
            verifyNoInteractions(tutorRepository, modelMapper);
        }
    }
}
