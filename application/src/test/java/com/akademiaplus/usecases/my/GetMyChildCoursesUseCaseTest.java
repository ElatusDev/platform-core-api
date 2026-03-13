/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
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

@DisplayName("GetMyChildCoursesUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyChildCoursesUseCaseTest {

    private static final Long TUTOR_PROFILE_ID = 100L;
    private static final Long MINOR_STUDENT_ID = 500L;
    private static final Long OTHER_MINOR_STUDENT_ID = 501L;

    @Mock private UserContextHolder userContextHolder;
    @Mock private MinorStudentRepository minorStudentRepository;

    private GetMyChildCoursesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyChildCoursesUseCase(userContextHolder, minorStudentRepository);
    }

    private MinorStudentDataModel buildMinorStudent(Long minorStudentId) {
        MinorStudentDataModel minor = new MinorStudentDataModel();
        minor.setMinorStudentId(minorStudentId);
        return minor;
    }

    @Nested
    @DisplayName("Child Course Retrieval")
    class ChildCourseRetrieval {

        @Test
        @DisplayName("Should return empty course list when child belongs to tutor")
        void shouldReturnEmptyCourseList_whenChildBelongsToTutor() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of(buildMinorStudent(MINOR_STUDENT_ID)));

            // When
            List<MyCourseDTO> result = useCase.execute(MINOR_STUDENT_ID);

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, minorStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when child does not belong to tutor")
        void shouldThrowEntityNotFoundException_whenChildNotBelongsToTutor() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of(buildMinorStudent(OTHER_MINOR_STUDENT_ID)));

            // When / Then
            assertThatThrownBy(() -> useCase.execute(MINOR_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.MINOR_STUDENT, MINOR_STUDENT_ID.toString()));

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileId();
            verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, minorStudentRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when tutor has no children")
        void shouldThrowEntityNotFoundException_whenTutorHasNoChildren() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of());

            // When / Then
            assertThatThrownBy(() -> useCase.execute(MINOR_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.MINOR_STUDENT, MINOR_STUDENT_ID.toString()));

            // Then — interactions
            verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, minorStudentRepository);
        }
    }
}
