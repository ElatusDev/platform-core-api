/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetAdultStudentResponseDTO;
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

@DisplayName("GetAdultStudentByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetAdultStudentByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ADULT_STUDENT_ID = 100L;

    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetAdultStudentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAdultStudentByIdUseCase(adultStudentRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when adult student is found")
        void shouldReturnMappedDto_whenAdultStudentFound() {
            // Given
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            AdultStudentDataModel adultStudent = new AdultStudentDataModel();
            adultStudent.setPersonPII(personPII);
            GetAdultStudentResponseDTO expectedDto = new GetAdultStudentResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(adultStudent));
            when(modelMapper.map(adultStudent, GetAdultStudentResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetAdultStudentResponseDTO result = useCase.get(ADULT_STUDENT_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(adultStudentRepository).findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID));
            verify(modelMapper).map(adultStudent, GetAdultStudentResponseDTO.class);
            verify(modelMapper).map(personPII, expectedDto);
            verifyNoMoreInteractions(tenantContextHolder, adultStudentRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when adult student not found")
        void shouldThrowEntityNotFoundException_whenAdultStudentNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(ADULT_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(ADULT_STUDENT_ID));
                    });
            verify(tenantContextHolder).getTenantId();
            verify(adultStudentRepository).findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID));
            verifyNoMoreInteractions(tenantContextHolder, adultStudentRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(ADULT_STUDENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetAdultStudentByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, adultStudentRepository, modelMapper);
        }
    }
}
