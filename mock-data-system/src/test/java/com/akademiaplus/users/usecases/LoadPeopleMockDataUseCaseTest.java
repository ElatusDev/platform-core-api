package com.akademiaplus.users.usecases;

import com.akademiaplus.customer.interfaceadapters.CustomerAuthRepository;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@DisplayName("LoadPeopleMockDataUseCase")
@ExtendWith(MockitoExtension.class)
class LoadPeopleMockDataUseCaseTest {

    @Mock private LoadEmployeeMockDataUseCase loadEmployeeUseCase;
    @Mock private LoadCollaboratorMockDataUseCase loadCollaboratorUseCase;
    @Mock private LoadAdultStudentMockDataUseCase loadAdultStudentUseCase;
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
        void shouldLoadDefaultFiftyRecords_whenCalledWithNoArgs() {
            // Given
            int defaultCount = 50;

            // When
            useCase.load();

            // Then
            verify(loadEmployeeUseCase).load(defaultCount);
            verify(loadCollaboratorUseCase).load(defaultCount);
            verify(loadAdultStudentUseCase).load(defaultCount);
        }

        @Test
        void shouldDelegateCountToAllSubUseCases_whenCalledWithCount() {
            // Given
            int customCount = 10;

            // When
            useCase.load(customCount);

            // Then
            verify(loadEmployeeUseCase).load(customCount);
            verify(loadCollaboratorUseCase).load(customCount);
            verify(loadAdultStudentUseCase).load(customCount);
        }
    }

    @Nested
    @DisplayName("Cleanup ordering")
    class CleanupOrdering {

        @Test
        void shouldCleanEntitiesBeforeAuthTables_whenCleaningUp() {
            // Given
            // useCase initialized in setUp

            // When
            useCase.cleanUp();

            // Then — entity tables cleaned before auth tables
            InOrder inOrder = inOrder(
                    loadEmployeeUseCase, loadCollaboratorUseCase,
                    loadAdultStudentUseCase, internalAuthCleanUp
            );
            inOrder.verify(loadEmployeeUseCase).clean();
            inOrder.verify(loadCollaboratorUseCase).clean();
            inOrder.verify(loadAdultStudentUseCase).clean();
            inOrder.verify(internalAuthCleanUp).clean();
        }

        @Test
        void shouldCleanAuthTablesBeforePiiTable_whenCleaningUp() {
            // Given
            // useCase initialized in setUp

            // When
            useCase.cleanUp();

            // Then — auth tables cleaned before PII
            InOrder inOrder = inOrder(
                    internalAuthCleanUp, customerAuthCleanUp, personPIICleanUp
            );
            inOrder.verify(internalAuthCleanUp).clean();
            inOrder.verify(customerAuthCleanUp).clean();
            inOrder.verify(personPIICleanUp).clean();
        }
    }
}
