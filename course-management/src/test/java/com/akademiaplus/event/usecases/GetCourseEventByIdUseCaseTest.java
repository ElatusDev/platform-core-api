/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
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

@DisplayName("GetCourseEventByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetCourseEventByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_EVENT_ID = 100L;

    @Mock private CourseEventRepository courseEventRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetCourseEventByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCourseEventByIdUseCase(courseEventRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when course event is found")
        void shouldReturnMappedDto_whenCourseEventFound() {
            // Given
            CourseEventDataModel courseEvent = new CourseEventDataModel();
            GetCourseEventResponseDTO expectedDto = new GetCourseEventResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(courseEventRepository.findById(new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID)))
                    .thenReturn(Optional.of(courseEvent));
            when(modelMapper.map(courseEvent, GetCourseEventResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetCourseEventResponseDTO result = useCase.get(COURSE_EVENT_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(courseEventRepository).findById(new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper).map(courseEvent, GetCourseEventResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course event not found")
        void shouldThrowEntityNotFoundException_whenCourseEventNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(courseEventRepository.findById(new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(COURSE_EVENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(exception -> {
                        EntityNotFoundException ex = (EntityNotFoundException) exception;
                        assertThat(ex.getEntityType()).isEqualTo(EntityType.COURSE_EVENT);
                        assertThat(ex.getEntityId()).isEqualTo(String.valueOf(COURSE_EVENT_ID));
                    });
            verify(tenantContextHolder).getTenantId();
            verify(courseEventRepository).findById(new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(COURSE_EVENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetCourseEventByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper);
        }
    }
}
