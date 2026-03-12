/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetMinorStudentByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetMinorStudentByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MINOR_STUDENT_ID = 100L;

    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetMinorStudentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMinorStudentByIdUseCase(minorStudentRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when minor student is found")
        void shouldReturnMappedDto_whenMinorStudentFound() {
            // Given
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            MinorStudentDataModel minorStudent = new MinorStudentDataModel();
            minorStudent.setPersonPII(personPII);
            GetMinorStudentResponseDTO expectedDto = new GetMinorStudentResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(minorStudentRepository.findById(new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID)))
                    .thenReturn(Optional.of(minorStudent));
            when(modelMapper.map(minorStudent, GetMinorStudentResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetMinorStudentResponseDTO result = useCase.get(MINOR_STUDENT_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);

            InOrder inOrder = inOrder(tenantContextHolder, minorStudentRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(minorStudentRepository, times(1)).findById(new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID));
            inOrder.verify(modelMapper, times(1)).map(minorStudent, GetMinorStudentResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(personPII, expectedDto);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when minor student not found")
        void shouldThrowEntityNotFoundException_whenMinorStudentNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(minorStudentRepository.findById(new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(MINOR_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(MINOR_STUDENT_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(minorStudentRepository, times(1)).findById(new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(modelMapper);
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
            assertThatThrownBy(() -> useCase.get(MINOR_STUDENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetMinorStudentByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder);
            verifyNoInteractions(minorStudentRepository, modelMapper);
        }
    }
}
