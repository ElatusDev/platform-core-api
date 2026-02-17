/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.customer.interfaceadapters.CustomerAuthRepository;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoadPeopleMockDataUseCase {

    private static final int DEFAULT_COUNT = 50;

    private final LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase;
    private final LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase;
    private final LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase;

    private final DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp;
    private final DataCleanUp<CustomerAuthDataModel, Long> customerAuthDataCleanUp;
    private final DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp;

    public LoadPeopleMockDataUseCase(
            LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase,
            LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase,
            LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase,
            InternalAuthRepository internalAuthRepository,
            DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp,
            CustomerAuthRepository customerAuthRepository,
            DataCleanUp<CustomerAuthDataModel, Long> customerAuthDataCleanUp,
            PersonPIIRepository personPIIRepository,
            DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp) {

        this.loadEmployeeMockDataUseCase = loadEmployeeMockDataUseCase;
        this.loadCollaboratorMockDataUseCase = loadCollaboratorMockDataUseCase;
        this.loadAdultStudentMockDataUseCase = loadAdultStudentMockDataUseCase;

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

    public void load(int count) {
        cleanUp();
        log.info("Loading {} employees, {} collaborators, {} adult students",
                count, count, count);
        loadEmployeeMockDataUseCase.load(count);
        loadCollaboratorMockDataUseCase.load(count);
        loadAdultStudentMockDataUseCase.load(count);
        log.info("Mock data generation complete");
    }

    /**
     * Cleanup order respects FK constraints:
     * 1. Entity tables (employees, collaborators, adult_students)
     * 2. Auth tables (internal_auths, customer_auths)
     * 3. PII table (person_piis) — referenced by all entity tables
     */
    public void cleanUp() {
        loadEmployeeMockDataUseCase.clean();
        loadCollaboratorMockDataUseCase.clean();
        loadAdultStudentMockDataUseCase.clean();
        internalAuthDataCleanUp.clean();
        customerAuthDataCleanUp.clean();
        personPIIDataCleanUp.clean();
    }
}
