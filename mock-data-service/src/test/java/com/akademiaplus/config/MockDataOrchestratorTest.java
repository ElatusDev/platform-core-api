package com.akademiaplus.config;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MockDataOrchestrator")
@ExtendWith(MockitoExtension.class)
class MockDataOrchestratorTest {

    private static final int TENANT_COUNT = 1;
    private static final int ENTITIES_PER_TENANT = 7;
    private static final int DEFAULT_TENANT_COUNT = 1;
    private static final int DEFAULT_ENTITIES_PER_TENANT = 50;
    private static final Long TENANT_ID = 1L;

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
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantContextHolder tenantContextHolder;

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

        TenantDataModel tenant = new TenantDataModel();
        tenant.setTenantId(TENANT_ID);
        lenient().when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        orchestrator = new MockDataOrchestrator(
                loaders, cleaners, hooks, tenantRepository, tenantContextHolder);
    }

    @Nested
    @DisplayName("generateAll(tenantCount, entitiesPerTenant)")
    class GenerateAllWithCount {

        @Test
        @DisplayName("shouldInvokeTenantLoaderWithTenantCount_whenGivenCounts")
        void shouldInvokeTenantLoaderWithTenantCount_whenGivenCounts() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(TENANT_COUNT).isEqualTo(1);

            // Then — interaction assertion in order: tenant loader invoked with tenant count
            InOrder inOrder = inOrder(tenantLoader);
            inOrder.verify(tenantLoader, times(1)).accept(TENANT_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldInvokeEntityLoadersWithEntitiesPerTenant_whenGivenCounts")
        void shouldInvokeEntityLoadersWithEntitiesPerTenant_whenGivenCounts() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(ENTITIES_PER_TENANT).isEqualTo(7);

            // Then — interaction assertions in order: all entity loaders invoked
            InOrder inOrder = inOrder(employeeLoader, collaboratorLoader,
                    adultStudentLoader, tutorLoader, minorStudentLoader);
            inOrder.verify(employeeLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(collaboratorLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(adultStudentLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(tutorLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(minorStudentLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldSetTenantContext_afterLoadingTenants")
        void shouldSetTenantContext_afterLoadingTenants() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(TENANT_ID).isEqualTo(1L);

            // Then — interaction assertions in order: load tenant → set context → load entities
            InOrder inOrder = inOrder(tenantLoader, tenantContextHolder, employeeLoader);
            inOrder.verify(tenantLoader, times(1)).accept(TENANT_COUNT);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TENANT_ID);
            inOrder.verify(employeeLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldInvokeAllCleaners_beforeAnyLoader")
        void shouldInvokeAllCleaners_beforeAnyLoader() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(TENANT_COUNT).isEqualTo(1);

            // Then — any cleaner must precede any loader
            InOrder inOrder = inOrder(minorStudentCleaner, tenantCleaner, tenantLoader, minorStudentLoader);
            inOrder.verify(minorStudentCleaner, times(1)).run();
            inOrder.verify(tenantCleaner, times(1)).run();
            inOrder.verify(tenantLoader, times(1)).accept(TENANT_COUNT);
            inOrder.verify(minorStudentLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldLoadTutor_beforeMinorStudent")
        void shouldLoadTutor_beforeMinorStudent() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(ENTITIES_PER_TENANT).isEqualTo(7);

            // Then — interaction assertions in order: tutor before minor student
            InOrder inOrder = inOrder(tutorLoader, minorStudentLoader);
            inOrder.verify(tutorLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(minorStudentLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldExecuteTutorPostLoadHook_betweenTutorLoadAndMinorStudentLoad")
        void shouldExecuteTutorPostLoadHook_betweenTutorLoadAndMinorStudentLoad() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(ENTITIES_PER_TENANT).isEqualTo(7);

            // Then — interaction assertions in order: tutor load → hook → minor student load
            InOrder inOrder = inOrder(tutorLoader, tutorPostLoadHook, minorStudentLoader);
            inOrder.verify(tutorLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verify(tutorPostLoadHook, times(1)).execute();
            inOrder.verify(minorStudentLoader, times(1)).accept(ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("shouldCleanMinorStudentsBeforeTenant_whenCleaningUp")
        void shouldCleanMinorStudentsBeforeTenant_whenCleaningUp() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — state assertion
            assertThat(TENANT_COUNT).isEqualTo(1);

            // Then — interaction assertions in order: clean minor students before tenant
            InOrder inOrder = inOrder(minorStudentCleaner, tenantCleaner);
            inOrder.verify(minorStudentCleaner, times(1)).run();
            inOrder.verify(tenantCleaner, times(1)).run();
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("generateAll() — default counts")
    class GenerateAllDefaultCount {

        @Test
        @DisplayName("shouldUseDefaultCounts_whenCalledWithNoArguments")
        void shouldUseDefaultCounts_whenCalledWithNoArguments() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll();

            // Then — state assertion
            assertThat(DEFAULT_TENANT_COUNT).isEqualTo(1);
            assertThat(DEFAULT_ENTITIES_PER_TENANT).isEqualTo(50);

            // Then — interaction assertions in order: tenant loader → entity loaders
            InOrder inOrder = inOrder(tenantLoader, employeeLoader, collaboratorLoader,
                    adultStudentLoader, tutorLoader, minorStudentLoader);
            inOrder.verify(tenantLoader, times(1)).accept(DEFAULT_TENANT_COUNT);
            inOrder.verify(employeeLoader, times(1)).accept(DEFAULT_ENTITIES_PER_TENANT);
            inOrder.verify(collaboratorLoader, times(1)).accept(DEFAULT_ENTITIES_PER_TENANT);
            inOrder.verify(adultStudentLoader, times(1)).accept(DEFAULT_ENTITIES_PER_TENANT);
            inOrder.verify(tutorLoader, times(1)).accept(DEFAULT_ENTITIES_PER_TENANT);
            inOrder.verify(minorStudentLoader, times(1)).accept(DEFAULT_ENTITIES_PER_TENANT);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("generateForTenant(tenantId, entitiesPerTenant)")
    class GenerateForTenant {

        public static final Long EXISTING_TENANT_ID = 1L;
        public static final Long NON_EXISTENT_TENANT_ID = 999L;
        public static final int TEST_COUNT = 10;

        @Test
        @DisplayName("Should load tenant-scoped entities when tenant exists")
        void shouldLoadTenantScopedEntities_whenTenantExists() {
            // Given
            TenantDataModel tenant = new TenantDataModel();
            tenant.setTenantId(EXISTING_TENANT_ID);
            when(tenantRepository.findById(EXISTING_TENANT_ID)).thenReturn(Optional.of(tenant));

            // When
            orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT);

            // Then — state assertion
            assertThat(EXISTING_TENANT_ID).isEqualTo(1L);

            // Then — interaction assertions in order: set context → load all entity types
            InOrder inOrder = inOrder(tenantContextHolder, employeeLoader, collaboratorLoader,
                    adultStudentLoader, tutorLoader, minorStudentLoader);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(EXISTING_TENANT_ID);
            inOrder.verify(employeeLoader, times(1)).accept(TEST_COUNT);
            inOrder.verify(collaboratorLoader, times(1)).accept(TEST_COUNT);
            inOrder.verify(adultStudentLoader, times(1)).accept(TEST_COUNT);
            inOrder.verify(tutorLoader, times(1)).accept(TEST_COUNT);
            inOrder.verify(minorStudentLoader, times(1)).accept(TEST_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when tenant does not exist")
        void shouldThrowEntityNotFoundException_whenTenantDoesNotExist() {
            // Given
            when(tenantRepository.findById(NON_EXISTENT_TENANT_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> orchestrator.generateForTenant(NON_EXISTENT_TENANT_ID, TEST_COUNT))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TENANT, String.valueOf(NON_EXISTENT_TENANT_ID)));

            // Then — interaction assertion: tenantRepository was called before the throw
            verify(tenantRepository, times(1)).findById(NON_EXISTENT_TENANT_ID);
            verifyNoMoreInteractions(tenantRepository);

            // Then — downstream mocks not reached on short-circuit path
            verifyNoInteractions(tenantContextHolder, tenantLoader,
                    employeeLoader, collaboratorLoader, adultStudentLoader,
                    tutorLoader, minorStudentLoader, tutorPostLoadHook,
                    tenantCleaner, tenantSequenceCleaner, personPiiCleaner,
                    internalAuthCleaner, customerAuthCleaner, employeeCleaner,
                    collaboratorCleaner, adultStudentCleaner, tutorCleaner,
                    minorStudentCleaner);
        }

        @Test
        @DisplayName("Should set tenant context before loading entities")
        void shouldSetTenantContext_whenGeneratingForTenant() {
            // Given
            TenantDataModel tenant = new TenantDataModel();
            tenant.setTenantId(EXISTING_TENANT_ID);
            when(tenantRepository.findById(EXISTING_TENANT_ID)).thenReturn(Optional.of(tenant));

            // When
            orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT);

            // Then — state assertion
            assertThat(EXISTING_TENANT_ID).isEqualTo(1L);

            // Then — interaction assertions in order: set context → load entities
            InOrder inOrder = inOrder(tenantContextHolder, employeeLoader);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(EXISTING_TENANT_ID);
            inOrder.verify(employeeLoader, times(1)).accept(TEST_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should skip TENANT entity type in load order")
        void shouldSkipTenantEntityType_whenLoadingEntities() {
            // Given
            TenantDataModel tenant = new TenantDataModel();
            tenant.setTenantId(EXISTING_TENANT_ID);
            when(tenantRepository.findById(EXISTING_TENANT_ID)).thenReturn(Optional.of(tenant));

            // When
            orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT);

            // Then — state assertion
            assertThat(EXISTING_TENANT_ID).isEqualTo(1L);

            // Then — tenant loader is never called; entity loaders are called instead
            verifyNoInteractions(tenantLoader);
            InOrder inOrder = inOrder(tenantContextHolder, employeeLoader);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(EXISTING_TENANT_ID);
            inOrder.verify(employeeLoader, times(1)).accept(TEST_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should execute post-load hooks for tenant-scoped entities")
        void shouldExecutePostLoadHooks_whenGeneratingForTenant() {
            // Given
            TenantDataModel tenant = new TenantDataModel();
            tenant.setTenantId(EXISTING_TENANT_ID);
            when(tenantRepository.findById(EXISTING_TENANT_ID)).thenReturn(Optional.of(tenant));

            // When
            orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT);

            // Then — state assertion
            assertThat(TEST_COUNT).isEqualTo(10);

            // Then — interaction assertions in order: tutor load → hook → minor student load
            InOrder inOrder = inOrder(tutorLoader, tutorPostLoadHook, minorStudentLoader);
            inOrder.verify(tutorLoader, times(1)).accept(TEST_COUNT);
            inOrder.verify(tutorPostLoadHook, times(1)).execute();
            inOrder.verify(minorStudentLoader, times(1)).accept(TEST_COUNT);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
