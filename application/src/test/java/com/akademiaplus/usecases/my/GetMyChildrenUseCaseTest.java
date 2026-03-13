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
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import openapi.akademiaplus.domain.my.dto.MyChildDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetMyChildrenUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyChildrenUseCaseTest {

    private static final Long TUTOR_PROFILE_ID = 100L;
    private static final Long MINOR_STUDENT_ID = 500L;
    private static final String CHILD_FIRST_NAME = "Alice";
    private static final String CHILD_LAST_NAME = "Doe";
    private static final LocalDate CHILD_BIRTH_DATE = LocalDate.of(2015, 5, 10);
    private static final LocalDate CHILD_ENTRY_DATE = LocalDate.of(2025, 9, 1);

    @Mock private UserContextHolder userContextHolder;
    @Mock private MinorStudentRepository minorStudentRepository;

    private GetMyChildrenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyChildrenUseCase(userContextHolder, minorStudentRepository);
    }

    private MinorStudentDataModel buildMinorStudent() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setFirstName(CHILD_FIRST_NAME);
        pii.setLastName(CHILD_LAST_NAME);

        MinorStudentDataModel minor = new MinorStudentDataModel();
        minor.setMinorStudentId(MINOR_STUDENT_ID);
        minor.setPersonPII(pii);
        minor.setBirthDate(CHILD_BIRTH_DATE);
        minor.setEntryDate(CHILD_ENTRY_DATE);
        return minor;
    }

    @Nested
    @DisplayName("Children Retrieval")
    class ChildrenRetrieval {

        @Test
        @DisplayName("Should return children when tutor has minor students")
        void shouldReturnChildren_whenMinorStudentsExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of(buildMinorStudent()));

            // When
            List<MyChildDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMinorStudentId()).isEqualTo(MINOR_STUDENT_ID);
            assertThat(result.get(0).getFirstName()).isEqualTo(CHILD_FIRST_NAME);
            assertThat(result.get(0).getLastName()).isEqualTo(CHILD_LAST_NAME);
            assertThat(result.get(0).getBirthDate()).isEqualTo(CHILD_BIRTH_DATE);
            assertThat(result.get(0).getEntryDate()).isEqualTo(CHILD_ENTRY_DATE);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, minorStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when tutor has no children")
        void shouldReturnEmptyList_whenNoChildrenExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyChildDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, minorStudentRepository);
        }

        @Test
        @DisplayName("Should handle null PII gracefully")
        void shouldHandleNullPii_whenPiiNotSet() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(TUTOR_PROFILE_ID);
            MinorStudentDataModel minor = new MinorStudentDataModel();
            minor.setMinorStudentId(MINOR_STUDENT_ID);
            minor.setBirthDate(CHILD_BIRTH_DATE);
            minor.setEntryDate(CHILD_ENTRY_DATE);
            when(minorStudentRepository.findByTutorId(TUTOR_PROFILE_ID))
                    .thenReturn(List.of(minor));

            // When
            List<MyChildDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMinorStudentId()).isEqualTo(MINOR_STUDENT_ID);
            assertThat(result.get(0).getFirstName()).isNull();
            assertThat(result.get(0).getLastName()).isNull();

            // Then — interactions
            verify(minorStudentRepository, times(1)).findByTutorId(TUTOR_PROFILE_ID);
        }
    }
}
