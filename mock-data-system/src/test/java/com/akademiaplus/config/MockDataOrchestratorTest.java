package com.akademiaplus.config;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
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
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.mockito.Mockito.*;

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

            // Then
            verify(tenantLoader).accept(TENANT_COUNT);
        }

        @Test
        @DisplayName("shouldInvokeEntityLoadersWithEntitiesPerTenant_whenGivenCounts")
        void shouldInvokeEntityLoadersWithEntitiesPerTenant_whenGivenCounts() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then
            verify(employeeLoader).accept(ENTITIES_PER_TENANT);
            verify(collaboratorLoader).accept(ENTITIES_PER_TENANT);
            verify(adultStudentLoader).accept(ENTITIES_PER_TENANT);
            verify(tutorLoader).accept(ENTITIES_PER_TENANT);
            verify(minorStudentLoader).accept(ENTITIES_PER_TENANT);
        }

        @Test
        @DisplayName("shouldSetTenantContext_afterLoadingTenants")
        void shouldSetTenantContext_afterLoadingTenants() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then
            InOrder inOrder = inOrder(tenantLoader, tenantContextHolder, employeeLoader);
            inOrder.verify(tenantLoader).accept(TENANT_COUNT);
            inOrder.verify(tenantContextHolder).setTenantId(TENANT_ID);
            inOrder.verify(employeeLoader).accept(ENTITIES_PER_TENANT);
        }

        @Test
        @DisplayName("shouldInvokeAllCleaners_beforeAnyLoader")
        void shouldInvokeAllCleaners_beforeAnyLoader() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then — any cleaner must precede any loader
            InOrder inOrder = inOrder(minorStudentCleaner, tenantCleaner, tenantLoader, minorStudentLoader);
            inOrder.verify(minorStudentCleaner).run();
            inOrder.verify(tenantCleaner).run();
            inOrder.verify(tenantLoader).accept(TENANT_COUNT);
            inOrder.verify(minorStudentLoader).accept(ENTITIES_PER_TENANT);
        }

        @Test
        @DisplayName("shouldLoadTutor_beforeMinorStudent")
        void shouldLoadTutor_beforeMinorStudent() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then
            InOrder inOrder = inOrder(tutorLoader, minorStudentLoader);
            inOrder.verify(tutorLoader).accept(ENTITIES_PER_TENANT);
            inOrder.verify(minorStudentLoader).accept(ENTITIES_PER_TENANT);
        }

        @Test
        @DisplayName("shouldExecuteTutorPostLoadHook_betweenTutorLoadAndMinorStudentLoad")
        void shouldExecuteTutorPostLoadHook_betweenTutorLoadAndMinorStudentLoad() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then
            InOrder inOrder = inOrder(tutorLoader, tutorPostLoadHook, minorStudentLoader);
            inOrder.verify(tutorLoader).accept(ENTITIES_PER_TENANT);
            inOrder.verify(tutorPostLoadHook).execute();
            inOrder.verify(minorStudentLoader).accept(ENTITIES_PER_TENANT);
        }

        @Test
        @DisplayName("shouldCleanMinorStudentsBeforeTenant_whenCleaningUp")
        void shouldCleanMinorStudentsBeforeTenant_whenCleaningUp() {
            // Given — orchestrator initialized in setUp

            // When
            orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT);

            // Then
            InOrder inOrder = inOrder(minorStudentCleaner, tenantCleaner);
            inOrder.verify(minorStudentCleaner).run();
            inOrder.verify(tenantCleaner).run();
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

            // Then
            verify(tenantLoader).accept(DEFAULT_TENANT_COUNT);
            verify(employeeLoader).accept(DEFAULT_ENTITIES_PER_TENANT);
            verify(collaboratorLoader).accept(DEFAULT_ENTITIES_PER_TENANT);
            verify(adultStudentLoader).accept(DEFAULT_ENTITIES_PER_TENANT);
            verify(tutorLoader).accept(DEFAULT_ENTITIES_PER_TENANT);
            verify(minorStudentLoader).accept(DEFAULT_ENTITIES_PER_TENANT);
        }
    }
}
