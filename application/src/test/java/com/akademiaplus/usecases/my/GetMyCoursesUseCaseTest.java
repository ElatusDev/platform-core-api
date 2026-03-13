/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
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
import static org.mockito.Mockito.*;

@DisplayName("GetMyCoursesUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyCoursesUseCaseTest {

    private static final Long PROFILE_ID = 100L;
    private static final Long COURSE_ID = 10L;
    private static final String COURSE_NAME = "Guitar 101";
    private static final String COURSE_DESC = "Beginner guitar class";
    private static final Integer MAX_CAPACITY = 20;

    @Mock private UserContextHolder userContextHolder;
    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;

    private GetMyCoursesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyCoursesUseCase(userContextHolder, membershipAdultStudentRepository);
    }

    private MembershipAdultStudentDataModel buildMembershipWithCourse() {
        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setCourseName(COURSE_NAME);
        course.setCourseDescription(COURSE_DESC);
        course.setMaxCapacity(MAX_CAPACITY);

        MembershipAdultStudentDataModel membership = new MembershipAdultStudentDataModel();
        membership.setCourse(course);
        return membership;
    }

    @Nested
    @DisplayName("Course Retrieval")
    class CourseRetrieval {

        @Test
        @DisplayName("Should return courses when student has memberships with courses")
        void shouldReturnCourses_whenMembershipsExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(buildMembershipWithCourse()));

            // When
            List<MyCourseDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseId()).isEqualTo(COURSE_ID);
            assertThat(result.get(0).getCourseName()).isEqualTo(COURSE_NAME);
            assertThat(result.get(0).getCourseDescription()).isEqualTo(COURSE_DESC);
            assertThat(result.get(0).getMaxCapacity()).isEqualTo(MAX_CAPACITY);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, membershipAdultStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when no memberships exist")
        void shouldReturnEmptyList_whenNoMembershipsExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyCourseDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileId();
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, membershipAdultStudentRepository);
        }

        @Test
        @DisplayName("Should filter out memberships with null course")
        void shouldFilterOutNullCourses_whenMembershipHasNoCourse() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            MembershipAdultStudentDataModel noCourse = new MembershipAdultStudentDataModel();
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(noCourse, buildMembershipWithCourse()));

            // When
            List<MyCourseDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseId()).isEqualTo(COURSE_ID);

            // Then — interactions
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(membershipAdultStudentRepository);
        }
    }
}
