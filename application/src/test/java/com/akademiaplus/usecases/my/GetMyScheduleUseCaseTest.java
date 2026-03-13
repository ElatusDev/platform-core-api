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
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import openapi.akademiaplus.domain.my.dto.MyScheduleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetMyScheduleUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyScheduleUseCaseTest {

    private static final Long PROFILE_ID = 100L;
    private static final Long COURSE_ID = 10L;
    private static final String COURSE_NAME = "Guitar 101";
    private static final Long SCHEDULE_ID = 50L;
    private static final String SCHEDULE_DAY = "Monday";
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(10, 30);

    @Mock private UserContextHolder userContextHolder;
    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;

    private GetMyScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyScheduleUseCase(userContextHolder, membershipAdultStudentRepository);
    }

    private MembershipAdultStudentDataModel buildMembershipWithSchedule() {
        ScheduleDataModel schedule = new ScheduleDataModel();
        schedule.setScheduleId(SCHEDULE_ID);
        schedule.setScheduleDay(SCHEDULE_DAY);
        schedule.setStartTime(START_TIME);
        schedule.setEndTime(END_TIME);

        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setCourseName(COURSE_NAME);
        course.setSchedules(List.of(schedule));

        MembershipAdultStudentDataModel membership = new MembershipAdultStudentDataModel();
        membership.setCourse(course);
        return membership;
    }

    @Nested
    @DisplayName("Schedule Retrieval")
    class ScheduleRetrieval {

        @Test
        @DisplayName("Should return schedule entries when courses have schedules")
        void shouldReturnScheduleEntries_whenCoursesHaveSchedules() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(buildMembershipWithSchedule()));

            // When
            List<MyScheduleDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScheduleId()).isEqualTo(SCHEDULE_ID);
            assertThat(result.get(0).getCourseId()).isEqualTo(COURSE_ID);
            assertThat(result.get(0).getCourseName()).isEqualTo(COURSE_NAME);
            assertThat(result.get(0).getScheduleDay()).isEqualTo(SCHEDULE_DAY);
            assertThat(result.get(0).getStartTime()).isEqualTo(START_TIME.toString());
            assertThat(result.get(0).getEndTime()).isEqualTo(END_TIME.toString());

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
            List<MyScheduleDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, membershipAdultStudentRepository);
        }

        @Test
        @DisplayName("Should handle null start and end times")
        void shouldHandleNullTimes_whenTimesNotSet() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);

            ScheduleDataModel schedule = new ScheduleDataModel();
            schedule.setScheduleId(SCHEDULE_ID);
            schedule.setScheduleDay(SCHEDULE_DAY);

            CourseDataModel course = new CourseDataModel();
            course.setCourseId(COURSE_ID);
            course.setCourseName(COURSE_NAME);
            course.setSchedules(List.of(schedule));

            MembershipAdultStudentDataModel membership = new MembershipAdultStudentDataModel();
            membership.setCourse(course);

            when(membershipAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(membership));

            // When
            List<MyScheduleDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStartTime()).isNull();
            assertThat(result.get(0).getEndTime()).isNull();

            // Then — interactions
            verify(membershipAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
        }
    }
}
