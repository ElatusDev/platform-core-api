/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.exception.CourseNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
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
            verify(tenantContextHolder).getTenantId();
            verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(modelMapper).map(course, GetCourseResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, courseRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw CourseNotFoundException when course not found")
        void shouldThrowCourseNotFoundException_whenCourseNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(COURSE_ID))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessage(String.valueOf(COURSE_ID));
            verify(tenantContextHolder).getTenantId();
            verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verifyNoMoreInteractions(tenantContextHolder, courseRepository, modelMapper);
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
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, courseRepository, modelMapper);
        }
    }
}
