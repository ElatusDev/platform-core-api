package com.akademiaplus.users.usecases;

import com.akademiaplus.customer.interfaceadapters.CustomerAuthRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.mock.users.MinorStudentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;
@DisplayName("LoadPeopleMockDataUseCase")
@ExtendWith(MockitoExtension.class)
class LoadPeopleMockDataUseCaseTest {

    @Mock private LoadEmployeeMockDataUseCase loadEmployeeUseCase;
    @Mock private LoadCollaboratorMockDataUseCase loadCollaboratorUseCase;
    @Mock private LoadAdultStudentMockDataUseCase loadAdultStudentUseCase;
    @Mock private LoadTutorMockDataUseCase loadTutorUseCase;
    @Mock private LoadMinorStudentMockDataUseCase loadMinorStudentUseCase;
    @Mock private TutorRepository tutorRepository;
    @Mock private MinorStudentFactory minorStudentFactory;
    @Mock private InternalAuthRepository internalAuthRepository;
    @Mock private CustomerAuthRepository customerAuthRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private DataCleanUp<InternalAuthDataModel, Long> internalAuthCleanUp;
    @Mock private DataCleanUp<CustomerAuthDataModel, Long> customerAuthCleanUp;
    @Mock private DataCleanUp<PersonPIIDataModel, Long> personPIICleanUp;

    private LoadPeopleMockDataUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoadPeopleMockDataUseCase(
                loadEmployeeUseCase,
                loadCollaboratorUseCase,
                loadAdultStudentUseCase,
                loadTutorUseCase,
                loadMinorStudentUseCase,
                tutorRepository,
                minorStudentFactory,
                internalAuthRepository,
                internalAuthCleanUp,
                customerAuthRepository,
                customerAuthCleanUp,
                personPIIRepository,
                personPIICleanUp
        );
    }

    @Nested
    @DisplayName("Loading mock data")
    class Loading {

        @Test
        @DisplayName("Should load default fifty records when called with no args")
        void shouldLoadDefaultFiftyRecords_whenCalledWithNoArgs() {
            // Given
            int defaultCount = 50;
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTutorId(1L);
            when(tutorRepository.findAll()).thenReturn(List.of(tutor));

            // When
            useCase.load();

            // Then
            verify(loadEmployeeUseCase).load(defaultCount);
            verify(loadCollaboratorUseCase).load(defaultCount);
            verify(loadAdultStudentUseCase).load(defaultCount);
            verify(loadTutorUseCase).load(defaultCount);
            verify(loadMinorStudentUseCase).load(defaultCount);
        }

        @Test
        @DisplayName("Should delegate count to all five sub-use cases when called with count")
        void shouldDelegateCountToAllSubUseCases_whenCalledWithCount() {
            // Given
            int customCount = 10;
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTutorId(1L);
            when(tutorRepository.findAll()).thenReturn(List.of(tutor));

            // When
            useCase.load(customCount);

            // Then
            verify(loadEmployeeUseCase).load(customCount);
            verify(loadCollaboratorUseCase).load(customCount);
            verify(loadAdultStudentUseCase).load(customCount);
            verify(loadTutorUseCase).load(customCount);
            verify(loadMinorStudentUseCase).load(customCount);
        }

        @Test
        @DisplayName("Should inject tutor IDs into minor student factory before loading minor students")
        void shouldInjectTutorIds_beforeLoadingMinorStudents() {
            // Given
            int count = 5;
            TutorDataModel tutor1 = new TutorDataModel();
            tutor1.setTutorId(10L);
            TutorDataModel tutor2 = new TutorDataModel();
            tutor2.setTutorId(20L);
            when(tutorRepository.findAll()).thenReturn(List.of(tutor1, tutor2));

            // When
            useCase.load(count);

            // Then
            InOrder inOrder = inOrder(loadTutorUseCase, tutorRepository, minorStudentFactory, loadMinorStudentUseCase);
            inOrder.verify(loadTutorUseCase).load(count);
            inOrder.verify(tutorRepository).findAll();
            inOrder.verify(minorStudentFactory).setAvailableTutorIds(List.of(10L, 20L));
            inOrder.verify(loadMinorStudentUseCase).load(count);
        }
    }

    @Nested
    @DisplayName("Cleanup ordering")
    class CleanupOrdering {

        @Test
        @DisplayName("Should clean minor students before tutors when cleaning up")
        void shouldCleanMinorStudentsBeforeTutors_whenCleaningUp() {
            // Given
            // useCase initialized in setUp

            // When
            useCase.cleanUp();

            // Then
            InOrder inOrder = inOrder(loadMinorStudentUseCase, loadTutorUseCase);
            inOrder.verify(loadMinorStudentUseCase).clean();
            inOrder.verify(loadTutorUseCase).clean();
        }

        @Test
        @DisplayName("Should clean entity tables before auth tables when cleaning up")
        void shouldCleanEntitiesBeforeAuthTables_whenCleaningUp() {
            // Given
            // useCase initialized in setUp

            // When
            useCase.cleanUp();

            // Then
            InOrder inOrder = inOrder(
                    loadMinorStudentUseCase, loadTutorUseCase,
                    loadEmployeeUseCase, loadCollaboratorUseCase,
                    loadAdultStudentUseCase, internalAuthCleanUp
            );
            inOrder.verify(loadMinorStudentUseCase).clean();
            inOrder.verify(loadTutorUseCase).clean();
            inOrder.verify(loadEmployeeUseCase).clean();
            inOrder.verify(loadCollaboratorUseCase).clean();
            inOrder.verify(loadAdultStudentUseCase).clean();
            inOrder.verify(internalAuthCleanUp).clean();
        }

        @Test
        @DisplayName("Should clean auth tables before PII table when cleaning up")
        void shouldCleanAuthTablesBeforePiiTable_whenCleaningUp() {
            // Given
            // useCase initialized in setUp

            // When
            useCase.cleanUp();

            // Then
            InOrder inOrder = inOrder(
                    internalAuthCleanUp, customerAuthCleanUp, personPIICleanUp
            );
            inOrder.verify(internalAuthCleanUp).clean();
            inOrder.verify(customerAuthCleanUp).clean();
            inOrder.verify(personPIICleanUp).clean();
        }
    }
}
