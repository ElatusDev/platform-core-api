package com.akademiaplus.config;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.mock.users.MinorStudentFactory;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.users.usecases.LoadAdultStudentMockDataUseCase;
import com.akademiaplus.users.usecases.LoadCollaboratorMockDataUseCase;
import com.akademiaplus.users.usecases.LoadEmployeeMockDataUseCase;
import com.akademiaplus.users.usecases.LoadMinorStudentMockDataUseCase;
import com.akademiaplus.users.usecases.LoadTenantMockDataUseCase;
import com.akademiaplus.users.usecases.LoadTutorMockDataUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MockDataRegistry")
@ExtendWith(MockitoExtension.class)
class MockDataRegistryTest {

    private static final int RECORD_COUNT = 5;
    private static final long TUTOR_ID_ONE = 10L;
    private static final long TUTOR_ID_TWO = 20L;

    @Mock private LoadTenantMockDataUseCase tenantUseCase;
    @Mock private LoadEmployeeMockDataUseCase employeeUseCase;
    @Mock private LoadCollaboratorMockDataUseCase collaboratorUseCase;
    @Mock private LoadAdultStudentMockDataUseCase adultStudentUseCase;
    @Mock private LoadTutorMockDataUseCase tutorUseCase;
    @Mock private LoadMinorStudentMockDataUseCase minorStudentUseCase;

    @Mock private DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceCleanUp;
    @Mock private DataCleanUp<InternalAuthDataModel, Long> internalAuthCleanUp;
    @Mock private DataCleanUp<CustomerAuthDataModel, Long> customerAuthCleanUp;
    @Mock private DataCleanUp<PersonPIIDataModel, Long> personPIICleanUp;

    @Mock private TutorRepository tutorRepository;
    @Mock private MinorStudentFactory minorStudentFactory;

    private MockDataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MockDataRegistry();
    }

    @Nested
    @DisplayName("mockDataLoaders bean")
    class MockDataLoaders {

        private Map<MockEntityType, IntConsumer> loaders;

        @BeforeEach
        void setUp() {
            loaders = registry.mockDataLoaders(
                    tenantUseCase, employeeUseCase, collaboratorUseCase,
                    adultStudentUseCase, tutorUseCase, minorStudentUseCase);
        }

        @Test
        @DisplayName("Should contain exactly the six loadable entity types")
        void shouldContainExactlySixLoadableEntityTypes() {
            // Given — loaders built in setUp

            // When & Then
            assertThat(loaders).containsOnlyKeys(
                    TENANT, EMPLOYEE, COLLABORATOR, ADULT_STUDENT, TUTOR, MINOR_STUDENT);
        }

        @Test
        @DisplayName("Should delegate tenant loader to tenant use case")
        void shouldDelegateTenantLoader_toTenantUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(TENANT).accept(RECORD_COUNT);

            // Then
            verify(tenantUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate employee loader to employee use case")
        void shouldDelegateEmployeeLoader_toEmployeeUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(EMPLOYEE).accept(RECORD_COUNT);

            // Then
            verify(employeeUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate collaborator loader to collaborator use case")
        void shouldDelegateCollaboratorLoader_toCollaboratorUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(COLLABORATOR).accept(RECORD_COUNT);

            // Then
            verify(collaboratorUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate adult student loader to adult student use case")
        void shouldDelegateAdultStudentLoader_toAdultStudentUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(ADULT_STUDENT).accept(RECORD_COUNT);

            // Then
            verify(adultStudentUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate tutor loader to tutor use case")
        void shouldDelegateTutorLoader_toTutorUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(TUTOR).accept(RECORD_COUNT);

            // Then
            verify(tutorUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate minor student loader to minor student use case")
        void shouldDelegateMinorStudentLoader_toMinorStudentUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(MINOR_STUDENT).accept(RECORD_COUNT);

            // Then
            verify(minorStudentUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — loaders built in setUp

            // When & Then
            assertThatThrownBy(() -> loaders.put(TENANT, count -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("mockDataCleaners bean")
    class MockDataCleaners {

        private Map<MockEntityType, Runnable> cleaners;

        @BeforeEach
        void setUp() {
            cleaners = registry.mockDataCleaners(
                    tenantUseCase, employeeUseCase, collaboratorUseCase,
                    adultStudentUseCase, tutorUseCase, minorStudentUseCase,
                    tenantSequenceCleanUp, internalAuthCleanUp,
                    customerAuthCleanUp, personPIICleanUp);
        }

        @Test
        @DisplayName("Should contain exactly the ten cleanable entity types")
        void shouldContainExactlyTenCleanableEntityTypes() {
            // Given — cleaners built in setUp

            // When & Then
            assertThat(cleaners).containsOnlyKeys(
                    TENANT, TENANT_SEQUENCE, PERSON_PII, INTERNAL_AUTH, CUSTOMER_AUTH,
                    EMPLOYEE, COLLABORATOR, ADULT_STUDENT, TUTOR, MINOR_STUDENT);
        }

        @Test
        @DisplayName("Should delegate tenant cleaner to tenant use case")
        void shouldDelegateTenantCleaner_toTenantUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT).run();

            // Then
            verify(tenantUseCase).clean();
        }

        @Test
        @DisplayName("Should delegate tenant sequence cleaner to tenant sequence data clean up")
        void shouldDelegateTenantSequenceCleaner_toTenantSequenceDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT_SEQUENCE).run();

            // Then
            verify(tenantSequenceCleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate person PII cleaner to person PII data clean up")
        void shouldDelegatePersonPiiCleaner_toPersonPiiDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(PERSON_PII).run();

            // Then
            verify(personPIICleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate internal auth cleaner to internal auth data clean up")
        void shouldDelegateInternalAuthCleaner_toInternalAuthDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(INTERNAL_AUTH).run();

            // Then
            verify(internalAuthCleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate customer auth cleaner to customer auth data clean up")
        void shouldDelegateCustomerAuthCleaner_toCustomerAuthDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(CUSTOMER_AUTH).run();

            // Then
            verify(customerAuthCleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate minor student cleaner to minor student use case")
        void shouldDelegateMinorStudentCleaner_toMinorStudentUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(MINOR_STUDENT).run();

            // Then
            verify(minorStudentUseCase).clean();
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — cleaners built in setUp

            // When & Then
            assertThatThrownBy(() -> cleaners.put(TENANT, () -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("mockDataPostLoadHooks bean")
    class MockDataPostLoadHooks {

        private Map<MockEntityType, MockDataPostLoadHook> hooks;

        @BeforeEach
        void setUp() {
            hooks = registry.mockDataPostLoadHooks(tutorRepository, minorStudentFactory);
        }

        @Test
        @DisplayName("Should contain only TUTOR hook")
        void shouldContainOnlyTutorHook() {
            // Given — hooks built in setUp

            // When & Then
            assertThat(hooks).containsOnlyKeys(TUTOR);
        }

        @Test
        @DisplayName("Should inject tutor IDs into minor student factory when tutor hook executes")
        void shouldInjectTutorIds_intoMinorStudentFactory_whenTutorHookExecutes() {
            // Given
            TutorDataModel tutor1 = new TutorDataModel();
            tutor1.setTutorId(TUTOR_ID_ONE);
            TutorDataModel tutor2 = new TutorDataModel();
            tutor2.setTutorId(TUTOR_ID_TWO);
            when(tutorRepository.findAll()).thenReturn(List.of(tutor1, tutor2));

            // When
            hooks.get(TUTOR).execute();

            // Then
            verify(tutorRepository).findAll();
            verify(minorStudentFactory).setAvailableTutorIds(List.of(TUTOR_ID_ONE, TUTOR_ID_TWO));
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — hooks built in setUp

            // When & Then
            assertThatThrownBy(() -> hooks.put(TENANT, () -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
