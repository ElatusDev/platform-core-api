/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.util.DataCleanUp;
import org.springframework.stereotype.Service;

@Service
public class LoadPeopleMockDataUseCase {

    private final DataCleanUp<InternalAuthDataModel, Integer> internalAuthDataCleanUp;
    private final DataCleanUp<PersonPIIDataModel, Integer> personPIIDataCleanUp;
    private final LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase;
    private final LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase;
    private final LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase;

    public LoadPeopleMockDataUseCase(InternalAuthRepository internalAuthRepository,
                                     DataCleanUp<InternalAuthDataModel, Integer> internalAuthDataCleanUp,
                                     PersonPIIRepository personPIIRepository,
                                     DataCleanUp<PersonPIIDataModel, Integer> personPIIDataCleanUp,
                                     LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase,
                                     LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase,
                                     LoadAdultStudentMockDataUseCase loadAdultStudentMockDataUseCase) {
        this.internalAuthDataCleanUp = internalAuthDataCleanUp;
        this.internalAuthDataCleanUp.setDataModel(InternalAuthDataModel.class);
        this.internalAuthDataCleanUp.setRepository(internalAuthRepository);

        this.personPIIDataCleanUp = personPIIDataCleanUp;
        this.personPIIDataCleanUp.setDataModel(PersonPIIDataModel.class);
        this.personPIIDataCleanUp.setRepository(personPIIRepository);

        this.loadEmployeeMockDataUseCase = loadEmployeeMockDataUseCase;
        this.loadCollaboratorMockDataUseCase = loadCollaboratorMockDataUseCase;
        this.loadAdultStudentMockDataUseCase = loadAdultStudentMockDataUseCase;
    }

    public void load() {
        cleanUp();
        loadEmployeeMockDataUseCase.load(50);
        loadCollaboratorMockDataUseCase.load(50);
        loadAdultStudentMockDataUseCase.load(500);
    }

    private void cleanUp() {
        loadEmployeeMockDataUseCase.clean();
        loadCollaboratorMockDataUseCase.clean();
        internalAuthDataCleanUp.clean();
        personPIIDataCleanUp.clean();
    }
}
