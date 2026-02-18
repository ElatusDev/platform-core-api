/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LoadPeopleMockDataUseCase {

    private static final int DEFAULT_COUNT = 50;

    private final LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase;
    private final LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase;
    private final LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase;
    private final LoadTutorMockDataUseCase loadTutorMockDataUseCase;
    private final LoadMinorStudentMockDataUseCase loadMinorStudentMockDataUseCase;

    private final TutorRepository tutorRepository;
    private final MinorStudentFactory minorStudentFactory;

    private final DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp;
    private final DataCleanUp<CustomerAuthDataModel, Long> customerAuthDataCleanUp;
    private final DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp;

    public LoadPeopleMockDataUseCase(
            LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase,
            LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase,
            LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase,
            LoadTutorMockDataUseCase loadTutorMockDataUseCase,
            LoadMinorStudentMockDataUseCase loadMinorStudentMockDataUseCase,
            TutorRepository tutorRepository,
            MinorStudentFactory minorStudentFactory,
            InternalAuthRepository internalAuthRepository,
            DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp,
            CustomerAuthRepository customerAuthRepository,
            DataCleanUp<CustomerAuthDataModel, Long> customerAuthDataCleanUp,
            PersonPIIRepository personPIIRepository,
            DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp) {

        this.loadEmployeeMockDataUseCase = loadEmployeeMockDataUseCase;
        this.loadCollaboratorMockDataUseCase = loadCollaboratorMockDataUseCase;
        this.loadAdultStudentMockDataUseCase = loadAdultStudentMockDataUseCase;
        this.loadTutorMockDataUseCase = loadTutorMockDataUseCase;
        this.loadMinorStudentMockDataUseCase = loadMinorStudentMockDataUseCase;

        this.tutorRepository = tutorRepository;
        this.minorStudentFactory = minorStudentFactory;

        this.internalAuthDataCleanUp = internalAuthDataCleanUp;
        this.internalAuthDataCleanUp.setDataModel(InternalAuthDataModel.class);
        this.internalAuthDataCleanUp.setRepository(internalAuthRepository);

        this.customerAuthDataCleanUp = customerAuthDataCleanUp;
        this.customerAuthDataCleanUp.setDataModel(CustomerAuthDataModel.class);
        this.customerAuthDataCleanUp.setRepository(customerAuthRepository);

        this.personPIIDataCleanUp = personPIIDataCleanUp;
        this.personPIIDataCleanUp.setDataModel(PersonPIIDataModel.class);
        this.personPIIDataCleanUp.setRepository(personPIIRepository);
    }

    public void load() {
        load(DEFAULT_COUNT);
    }

    /**
     * Loads mock data for all people entity types.
     * <p>
     * Tutors are loaded before minor students because a minor student
     * requires an existing tutor FK. After tutors are persisted,
     * their IDs are collected and passed to the minor student factory
     * so each generated minor student references a valid tutor.
     * <p>
     * Callers are responsible for invoking {@link #cleanUp()} before
     * this method if a fresh load is desired. Cleanup is not called
     * internally because the controller orchestrates the global cleanup
     * order across entity types (e.g., tenants must be cleaned after
     * people due to FK constraints).
     */
    public void load(int count) {
        log.info("Loading {} records per entity type: employees, collaborators, adult students, tutors, minor students",
                count);
        loadEmployeeMockDataUseCase.load(count);
        loadCollaboratorMockDataUseCase.load(count);
        loadAdultStudentMockDataUseCase.load(count);
        loadTutorMockDataUseCase.load(count);

        List<Long> tutorIds = tutorRepository.findAll().stream()
                .map(TutorDataModel::getTutorId)
                .toList();
        minorStudentFactory.setAvailableTutorIds(tutorIds);
        loadMinorStudentMockDataUseCase.load(count);

        log.info("Mock data generation complete");
    }

    /**
     * Cleanup order respects FK constraints:
     * 1. Minor students (FK → tutors, must be first)
     * 2. Entity tables (tutors, employees, collaborators, adult_students)
     * 3. Auth tables (internal_auths, customer_auths)
     * 4. PII table (person_piis) — referenced by all entity tables
     */
    public void cleanUp() {
        loadMinorStudentMockDataUseCase.clean();
        loadTutorMockDataUseCase.clean();
        loadEmployeeMockDataUseCase.clean();
        loadCollaboratorMockDataUseCase.clean();
        loadAdultStudentMockDataUseCase.clean();
        internalAuthDataCleanUp.clean();
        customerAuthDataCleanUp.clean();
        personPIIDataCleanUp.clean();
    }
}
