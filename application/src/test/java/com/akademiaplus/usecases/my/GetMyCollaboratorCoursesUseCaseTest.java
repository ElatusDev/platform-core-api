/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import openapi.akademiaplus.domain.my.dto.MyCourseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetMyCollaboratorCoursesUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyCollaboratorCoursesUseCaseTest {

    private static final Long COLLABORATOR_PROFILE_ID = 200L;
    private static final Long COURSE_ID = 10L;
    private static final String COURSE_NAME = "Guitar 101";
    private static final String COURSE_DESC = "Beginner guitar class";
    private static final Integer MAX_CAPACITY = 20;

    @Mock private UserContextHolder userContextHolder;
    @Mock private CourseRepository courseRepository;

    private GetMyCollaboratorCoursesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyCollaboratorCoursesUseCase(userContextHolder, courseRepository);
    }

    private CourseDataModel buildCourse() {
        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setCourseName(COURSE_NAME);
        course.setCourseDescription(COURSE_DESC);
        course.setMaxCapacity(MAX_CAPACITY);
        return course;
    }

    @Nested
    @DisplayName("Course Retrieval")
    class CourseRetrieval {

        @Test
        @DisplayName("Should return courses when collaborator has available courses")
        void shouldReturnCourses_whenCollaboratorHasAvailableCourses() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(courseRepository.findByAvailableCollaboratorId(COLLABORATOR_PROFILE_ID))
                    .thenReturn(List.of(buildCourse()));

            // When
            List<MyCourseDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            MyCourseDTO dto = result.get(0);
            assertThat(dto.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(dto.getCourseName()).isEqualTo(COURSE_NAME);
            assertThat(dto.getCourseDescription()).isEqualTo(COURSE_DESC);
            assertThat(dto.getMaxCapacity()).isEqualTo(MAX_CAPACITY);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, courseRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(courseRepository, times(1)).findByAvailableCollaboratorId(COLLABORATOR_PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when collaborator has no available courses")
        void shouldReturnEmptyList_whenNoCoursesExist() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(courseRepository.findByAvailableCollaboratorId(COLLABORATOR_PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyCourseDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileType();
            verify(userContextHolder, times(1)).requireProfileId();
            verify(courseRepository, times(1)).findByAvailableCollaboratorId(COLLABORATOR_PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, courseRepository);
        }
    }

    @Nested
    @DisplayName("Profile Type Validation")
    class ProfileTypeValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when profile type is not COLLABORATOR")
        void shouldThrowIllegalStateException_whenNotCollaborator() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);

            // When / Then
            assertThatThrownBy(() -> useCase.execute())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(GetMyCollaboratorCoursesUseCase.ERROR_NOT_COLLABORATOR);

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileType();
            verifyNoMoreInteractions(userContextHolder, courseRepository);
        }
    }
}
