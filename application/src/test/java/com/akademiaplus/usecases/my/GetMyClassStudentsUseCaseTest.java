/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.my.dto.MyClassStudentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetMyClassStudentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyClassStudentsUseCaseTest {

    private static final Long COLLABORATOR_PROFILE_ID = 200L;
    private static final Long OTHER_COLLABORATOR_ID = 300L;
    private static final Long CLASS_ID = 10L;
    private static final Long TENANT_ID = 1L;
    private static final Long ADULT_STUDENT_ID = 500L;
    private static final Long MINOR_STUDENT_ID = 600L;

    @Mock private UserContextHolder userContextHolder;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private CourseEventRepository courseEventRepository;

    private GetMyClassStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyClassStudentsUseCase(userContextHolder, tenantContextHolder, courseEventRepository);
    }

    private CourseEventDataModel buildEventOwnedBy(Long collaboratorId) {
        PersonPIIDataModel adultPii = new PersonPIIDataModel();
        adultPii.setFirstName("Alice");
        adultPii.setLastName("Smith");

        AdultStudentDataModel adult = new AdultStudentDataModel();
        adult.setAdultStudentId(ADULT_STUDENT_ID);
        adult.setPersonPII(adultPii);

        PersonPIIDataModel minorPii = new PersonPIIDataModel();
        minorPii.setFirstName("Bob");
        minorPii.setLastName("Jones");

        MinorStudentDataModel minor = new MinorStudentDataModel();
        minor.setMinorStudentId(MINOR_STUDENT_ID);
        minor.setPersonPII(minorPii);

        CourseEventDataModel event = new CourseEventDataModel();
        event.setCourseEventId(CLASS_ID);
        event.setCollaboratorId(collaboratorId);
        event.setAdultAttendees(List.of(adult));
        event.setMinorAttendees(List.of(minor));
        return event;
    }

    @Nested
    @DisplayName("Student Retrieval")
    class StudentRetrieval {

        @Test
        @DisplayName("Should return adult and minor students when collaborator owns class")
        void shouldReturnStudents_whenCollaboratorOwnsClass() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, CLASS_ID);
            when(courseEventRepository.findById(compositeId))
                    .thenReturn(Optional.of(buildEventOwnedBy(COLLABORATOR_PROFILE_ID)));

            // When
            List<MyClassStudentDTO> result = useCase.execute(CLASS_ID);

            // Then — state
            assertThat(result).hasSize(2);

            MyClassStudentDTO adultDto = result.stream()
                    .filter(s -> s.getStudentType() == MyClassStudentDTO.StudentTypeEnum.ADULT)
                    .findFirst().orElseThrow();
            assertThat(adultDto.getStudentId()).isEqualTo(ADULT_STUDENT_ID);
            assertThat(adultDto.getFirstName()).isEqualTo("Alice");
            assertThat(adultDto.getLastName()).isEqualTo("Smith");

            MyClassStudentDTO minorDto = result.stream()
                    .filter(s -> s.getStudentType() == MyClassStudentDTO.StudentTypeEnum.MINOR)
                    .findFirst().orElseThrow();
            assertThat(minorDto.getStudentId()).isEqualTo(MINOR_STUDENT_ID);
            assertThat(minorDto.getFirstName()).isEqualTo("Bob");
            assertThat(minorDto.getLastName()).isEqualTo("Jones");

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, tenantContextHolder, courseEventRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(courseEventRepository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when class has no attendees")
        void shouldReturnEmptyList_whenNoAttendees() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel event = new CourseEventDataModel();
            event.setCourseEventId(CLASS_ID);
            event.setCollaboratorId(COLLABORATOR_PROFILE_ID);
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, CLASS_ID);
            when(courseEventRepository.findById(compositeId)).thenReturn(Optional.of(event));

            // When
            List<MyClassStudentDTO> result = useCase.execute(CLASS_ID);

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(courseEventRepository, times(1)).findById(compositeId);
        }
    }

    @Nested
    @DisplayName("Ownership Validation")
    class OwnershipValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when class belongs to another collaborator")
        void shouldThrowEntityNotFoundException_whenClassBelongsToAnotherCollaborator() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, CLASS_ID);
            when(courseEventRepository.findById(compositeId))
                    .thenReturn(Optional.of(buildEventOwnedBy(OTHER_COLLABORATOR_ID)));

            // When / Then
            assertThatThrownBy(() -> useCase.execute(CLASS_ID))
                    .isInstanceOf(EntityNotFoundException.class);

            // Then — interactions
            verify(courseEventRepository, times(1)).findById(compositeId);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when class does not exist")
        void shouldThrowEntityNotFoundException_whenClassNotFound() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, CLASS_ID);
            when(courseEventRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.execute(CLASS_ID))
                    .isInstanceOf(EntityNotFoundException.class);

            // Then — interactions
            verify(courseEventRepository, times(1)).findById(compositeId);
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
            assertThatThrownBy(() -> useCase.execute(CLASS_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(GetMyClassStudentsUseCase.ERROR_NOT_COLLABORATOR);

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileType();
            verifyNoMoreInteractions(userContextHolder, tenantContextHolder, courseEventRepository);
        }
    }
}
