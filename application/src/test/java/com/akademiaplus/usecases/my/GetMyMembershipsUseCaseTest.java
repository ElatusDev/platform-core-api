/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import openapi.akademiaplus.domain.my.dto.MyMembershipDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetMyMembershipsUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyMembershipsUseCaseTest {

    private static final Long PROFILE_ID = 100L;
    private static final Long MEMBERSHIP_STUDENT_ID = 30L;
    private static final Long COURSE_ID = 10L;
    private static final String COURSE_NAME = "Guitar 101";
    private static final String MEMBERSHIP_TYPE = "MONTHLY";
    private static final BigDecimal FEE = new BigDecimal("49.99");
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 2, 1);

    @Mock private UserContextHolder userContextHolder;
    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;

    private GetMyMembershipsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyMembershipsUseCase(userContextHolder, membershipAdultStudentRepository);
    }

    private MembershipAdultStudentDataModel buildFullMembership() {
        MembershipDataModel membership = new MembershipDataModel();
        membership.setMembershipType(MEMBERSHIP_TYPE);
        membership.setFee(FEE);

        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setCourseName(COURSE_NAME);

        MembershipAdultStudentDataModel mas = new MembershipAdultStudentDataModel();
        mas.setMembershipAdultStudentId(MEMBERSHIP_STUDENT_ID);
        mas.setMembership(membership);
        mas.setCourse(course);
        mas.setCourseId(COURSE_ID);
        mas.setStartDate(START_DATE);
        mas.setDueDate(DUE_DATE);
        return mas;
    }

    @Nested
    @DisplayName("Membership Retrieval")
    class MembershipRetrieval {

        @Test
        @DisplayName("Should return memberships with full details")
        void shouldReturnMemberships_whenExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(buildFullMembership()));

            // When
            List<MyMembershipDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMembershipAdultStudentId()).isEqualTo(MEMBERSHIP_STUDENT_ID);
            assertThat(result.get(0).getMembershipType()).isEqualTo(MEMBERSHIP_TYPE);
            assertThat(result.get(0).getFee()).isEqualTo(FEE.doubleValue());
            assertThat(result.get(0).getStartDate()).isEqualTo(START_DATE);
            assertThat(result.get(0).getDueDate()).isEqualTo(DUE_DATE);
            assertThat(result.get(0).getCourseId()).isEqualTo(COURSE_ID);
            assertThat(result.get(0).getCourseName()).isEqualTo(COURSE_NAME);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, membershipAdultStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should handle null membership and course gracefully")
        void shouldHandleNullMembershipAndCourse_whenNotLinked() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            MembershipAdultStudentDataModel mas = new MembershipAdultStudentDataModel();
            mas.setMembershipAdultStudentId(MEMBERSHIP_STUDENT_ID);
            mas.setStartDate(START_DATE);
            mas.setDueDate(DUE_DATE);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(mas));

            // When
            List<MyMembershipDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMembershipType()).isNull();
            assertThat(result.get(0).getFee()).isNull();
            assertThat(result.get(0).getCourseName()).isNull();

            // Then — interactions
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(membershipAdultStudentRepository);
        }

        @Test
        @DisplayName("Should return empty list when no memberships")
        void shouldReturnEmptyList_whenNoMemberships() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyMembershipDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, membershipAdultStudentRepository);
        }
    }
}
