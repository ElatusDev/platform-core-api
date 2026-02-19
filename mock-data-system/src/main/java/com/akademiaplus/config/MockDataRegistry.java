/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Maps each {@link MockEntityType} to its loader, cleanup, and post-load hook beans.
 *
 * <p>The orchestrator consumes these maps to drive FK-safe execution
 * without hard-coding entity ordering.</p>
 *
 * <p>Three bean maps are produced:</p>
 * <ul>
 *   <li>{@code mockDataLoaders} — {@link IntConsumer} accepting the record count;
 *       one entry per {@linkplain MockEntityType#isLoadable() loadable} entity.</li>
 *   <li>{@code mockDataCleaners} — {@link Runnable} performing the cleanup;
 *       one entry per {@linkplain MockEntityType#isCleanable() cleanable} entity.</li>
 *   <li>{@code mockDataPostLoadHooks} — {@link MockDataPostLoadHook} for wiring
 *       generated IDs into downstream factories (currently only {@code TUTOR}).</li>
 * </ul>
 */
@Configuration
public class MockDataRegistry {

    @Bean
    public Map<MockEntityType, IntConsumer> mockDataLoaders(
            LoadTenantMockDataUseCase tenantUseCase,
            LoadEmployeeMockDataUseCase employeeUseCase,
            LoadCollaboratorMockDataUseCase collaboratorUseCase,
            LoadAdultStudentMockDataUseCase adultStudentUseCase,
            LoadTutorMockDataUseCase tutorUseCase,
            LoadMinorStudentMockDataUseCase minorStudentUseCase) {

        Map<MockEntityType, IntConsumer> loaders = new EnumMap<>(MockEntityType.class);
        loaders.put(MockEntityType.TENANT, tenantUseCase::load);
        loaders.put(MockEntityType.EMPLOYEE, employeeUseCase::load);
        loaders.put(MockEntityType.COLLABORATOR, collaboratorUseCase::load);
        loaders.put(MockEntityType.ADULT_STUDENT, adultStudentUseCase::load);
        loaders.put(MockEntityType.TUTOR, tutorUseCase::load);
        loaders.put(MockEntityType.MINOR_STUDENT, minorStudentUseCase::load);
        return Collections.unmodifiableMap(loaders);
    }

    @Bean
    public Map<MockEntityType, Runnable> mockDataCleaners(
            LoadTenantMockDataUseCase tenantUseCase,
            LoadEmployeeMockDataUseCase employeeUseCase,
            LoadCollaboratorMockDataUseCase collaboratorUseCase,
            LoadAdultStudentMockDataUseCase adultStudentUseCase,
            LoadTutorMockDataUseCase tutorUseCase,
            LoadMinorStudentMockDataUseCase minorStudentUseCase,
            @Qualifier("tenantSequenceDataCleanUp")
            DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceCleanUp,
            @Qualifier("internalAuthDataCleanUp")
            DataCleanUp<InternalAuthDataModel, Long> internalAuthCleanUp,
            @Qualifier("customerAuthDataCleanUp")
            DataCleanUp<CustomerAuthDataModel, Long> customerAuthCleanUp,
            @Qualifier("personPIIDataCleanUp")
            DataCleanUp<PersonPIIDataModel, Long> personPIICleanUp) {

        Map<MockEntityType, Runnable> cleaners = new EnumMap<>(MockEntityType.class);
        cleaners.put(MockEntityType.TENANT, tenantUseCase::clean);
        cleaners.put(MockEntityType.TENANT_SEQUENCE, tenantSequenceCleanUp::clean);
        cleaners.put(MockEntityType.PERSON_PII, personPIICleanUp::clean);
        cleaners.put(MockEntityType.INTERNAL_AUTH, internalAuthCleanUp::clean);
        cleaners.put(MockEntityType.CUSTOMER_AUTH, customerAuthCleanUp::clean);
        cleaners.put(MockEntityType.EMPLOYEE, employeeUseCase::clean);
        cleaners.put(MockEntityType.COLLABORATOR, collaboratorUseCase::clean);
        cleaners.put(MockEntityType.ADULT_STUDENT, adultStudentUseCase::clean);
        cleaners.put(MockEntityType.TUTOR, tutorUseCase::clean);
        cleaners.put(MockEntityType.MINOR_STUDENT, minorStudentUseCase::clean);
        return Collections.unmodifiableMap(cleaners);
    }

    @Bean
    public Map<MockEntityType, MockDataPostLoadHook> mockDataPostLoadHooks(
            TenantRepository tenantRepository,
            TenantContextHolder tenantContextHolder,
            TutorRepository tutorRepository,
            MinorStudentFactory minorStudentFactory) {

        Map<MockEntityType, MockDataPostLoadHook> hooks = new EnumMap<>(MockEntityType.class);
        hooks.put(MockEntityType.TENANT, () -> {
            Long firstTenantId = tenantRepository.findAll().stream()
                    .map(TenantDataModel::getTenantId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No tenants found after TENANT load"));
            tenantContextHolder.setTenantId(firstTenantId);
        });
        hooks.put(MockEntityType.TUTOR, () -> {
            List<Long> tutorIds = tutorRepository.findAll().stream()
                    .map(TutorDataModel::getTutorId)
                    .toList();
            minorStudentFactory.setAvailableTutorIds(tutorIds);
        });
        return Collections.unmodifiableMap(hooks);
    }
}
