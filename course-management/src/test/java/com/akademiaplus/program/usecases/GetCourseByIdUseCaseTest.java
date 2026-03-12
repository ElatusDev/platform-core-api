/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
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

@DisplayName("GetCourseByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetCourseByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_ID = 100L;

    @Mock private CourseRepository courseRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetCourseByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCourseByIdUseCase(courseRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when course is found")
        void shouldReturnMappedDto_whenCourseFound() {
            // Given
            CourseDataModel course = new CourseDataModel();
            GetCourseResponseDTO expectedDto = new GetCourseResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.of(course));
            when(modelMapper.map(course, GetCourseResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetCourseResponseDTO result = useCase.get(COURSE_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            InOrder inOrder = inOrder(tenantContextHolder, courseRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(modelMapper, times(1)).map(course, GetCourseResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course not found")
        void shouldThrowEntityNotFoundException_whenCourseNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(COURSE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(exception -> {
                        EntityNotFoundException ex = (EntityNotFoundException) exception;
                        assertThat(ex.getEntityType()).isEqualTo(EntityType.COURSE);
                        assertThat(ex.getEntityId()).isEqualTo(String.valueOf(COURSE_ID));
                    });

            verify(tenantContextHolder, times(1)).getTenantId();
            verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, courseRepository);
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
            assertThatThrownBy(() -> useCase.get(COURSE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetCourseByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(courseRepository, modelMapper);
            verifyNoMoreInteractions(tenantContextHolder);
        }
    }
}
