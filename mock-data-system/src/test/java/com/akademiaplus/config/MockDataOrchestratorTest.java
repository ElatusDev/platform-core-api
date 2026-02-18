package com.akademiaplus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.mockito.Mockito.*;

@DisplayName("MockDataOrchestrator")
@ExtendWith(MockitoExtension.class)
class MockDataOrchestratorTest {

    private static final int CUSTOM_COUNT = 7;
    private static final int DEFAULT_COUNT = 50;

    @Mock private IntConsumer tenantLoader;
    @Mock private IntConsumer employeeLoader;
    @Mock private IntConsumer collaboratorLoader;
    @Mock private IntConsumer adultStudentLoader;
    @Mock private IntConsumer tutorLoader;
    @Mock private IntConsumer minorStudentLoader;

    @Mock private Runnable tenantCleaner;
    @Mock private Runnable tenantSequenceCleaner;
    @Mock private Runnable personPiiCleaner;
    @Mock private Runnable internalAuthCleaner;
    @Mock private Runnable customerAuthCleaner;
    @Mock private Runnable employeeCleaner;
    @Mock private Runnable collaboratorCleaner;
    @Mock private Runnable adultStudentCleaner;
    @Mock private Runnable tutorCleaner;
    @Mock private Runnable minorStudentCleaner;

    @Mock private MockDataPostLoadHook tutorPostLoadHook;

    private MockDataOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        Map<MockEntityType, IntConsumer> loaders = new EnumMap<>(MockEntityType.class);
        loaders.put(TENANT, tenantLoader);
        loaders.put(EMPLOYEE, employeeLoader);
        loaders.put(COLLABORATOR, collaboratorLoader);
        loaders.put(ADULT_STUDENT, adultStudentLoader);
        loaders.put(TUTOR, tutorLoader);
        loaders.put(MINOR_STUDENT, minorStudentLoader);

        Map<MockEntityType, Runnable> cleaners = new EnumMap<>(MockEntityType.class);
        cleaners.put(TENANT, tenantCleaner);
        cleaners.put(TENANT_SEQUENCE, tenantSequenceCleaner);
        cleaners.put(PERSON_PII, personPiiCleaner);
        cleaners.put(INTERNAL_AUTH, internalAuthCleaner);
        cleaners.put(CUSTOMER_AUTH, customerAuthCleaner);
        cleaners.put(EMPLOYEE, employeeCleaner);
        cleaners.put(COLLABORATOR, collaboratorCleaner);
        cleaners.put(ADULT_STUDENT, adultStudentCleaner);
        cleaners.put(TUTOR, tutorCleaner);
        cleaners.put(MINOR_STUDENT, minorStudentCleaner);

        Map<MockEntityType, MockDataPostLoadHook> hooks = new EnumMap<>(MockEntityType.class);
        hooks.put(TUTOR, tutorPostLoadHook);

        orchestrator = new MockDataOrchestrator(loaders, cleaners, hooks);
    }

    @Nested
    @DisplayName("generateAll(int)")
    class GenerateAllWithCount {

        @Test
        @DisplayName("Should invoke all loaders with given count")
        void shouldInvokeAllLoaders_withGivenCount() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then
            verify(tenantLoader).accept(CUSTOM_COUNT);
            verify(employeeLoader).accept(CUSTOM_COUNT);
            verify(collaboratorLoader).accept(CUSTOM_COUNT);
            verify(adultStudentLoader).accept(CUSTOM_COUNT);
            verify(tutorLoader).accept(CUSTOM_COUNT);
            verify(minorStudentLoader).accept(CUSTOM_COUNT);
        }

        @Test
        @DisplayName("Should invoke all cleaners before any loader")
        void shouldInvokeAllCleaners_beforeAnyLoader() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then — any cleaner must precede any loader
            InOrder inOrder = inOrder(tenantCleaner, minorStudentCleaner, tenantLoader, minorStudentLoader);
            inOrder.verify(minorStudentCleaner).run();
            inOrder.verify(tenantCleaner).run();
            inOrder.verify(tenantLoader).accept(CUSTOM_COUNT);
            inOrder.verify(minorStudentLoader).accept(CUSTOM_COUNT);
        }

        @Test
        @DisplayName("Should load tenant before any people entity")
        void shouldLoadTenant_beforeAnyPeopleEntity() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then
            InOrder inOrder = inOrder(tenantLoader, employeeLoader, tutorLoader, minorStudentLoader);
            inOrder.verify(tenantLoader).accept(CUSTOM_COUNT);
            inOrder.verify(employeeLoader).accept(CUSTOM_COUNT);
        }

        @Test
        @DisplayName("Should load tutor before minor student")
        void shouldLoadTutor_beforeMinorStudent() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then
            InOrder inOrder = inOrder(tutorLoader, minorStudentLoader);
            inOrder.verify(tutorLoader).accept(CUSTOM_COUNT);
            inOrder.verify(minorStudentLoader).accept(CUSTOM_COUNT);
        }

        @Test
        @DisplayName("Should execute tutor post-load hook between tutor load and minor student load")
        void shouldExecuteTutorPostLoadHook_betweenTutorLoadAndMinorStudentLoad() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then
            InOrder inOrder = inOrder(tutorLoader, tutorPostLoadHook, minorStudentLoader);
            inOrder.verify(tutorLoader).accept(CUSTOM_COUNT);
            inOrder.verify(tutorPostLoadHook).execute();
            inOrder.verify(minorStudentLoader).accept(CUSTOM_COUNT);
        }

        @Test
        @DisplayName("Should clean minor students before tenant when cleaning up")
        void shouldCleanMinorStudentsBeforeTenant_whenCleaningUp() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(CUSTOM_COUNT);

            // Then
            InOrder inOrder = inOrder(minorStudentCleaner, tenantCleaner);
            inOrder.verify(minorStudentCleaner).run();
            inOrder.verify(tenantCleaner).run();
        }
    }

    @Nested
    @DisplayName("generateAll() — default count")
    class GenerateAllDefaultCount {

        @Test
        @DisplayName("Should use default count of fifty when called with no arguments")
        void shouldUseDefaultCountOfFifty_whenCalledWithNoArguments() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll();

            // Then
            verify(tenantLoader).accept(DEFAULT_COUNT);
            verify(employeeLoader).accept(DEFAULT_COUNT);
            verify(collaboratorLoader).accept(DEFAULT_COUNT);
            verify(adultStudentLoader).accept(DEFAULT_COUNT);
            verify(tutorLoader).accept(DEFAULT_COUNT);
            verify(minorStudentLoader).accept(DEFAULT_COUNT);
        }
    }
}
