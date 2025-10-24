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
import com.akademiaplus.util.base.DataCleanUp;
import org.springframework.stereotype.Service;

@Service
public class LoadPeopleMockDataUseCase {
    private final DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp;
    private final DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp;
    private final LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase;

    public LoadPeopleMockDataUseCase(InternalAuthRepository internalAuthRepository,
                                     DataCleanUp<InternalAuthDataModel, Long> internalAuthDataCleanUp,
                                     PersonPIIRepository personPIIRepository,
                                     DataCleanUp<PersonPIIDataModel, Long> personPIIDataCleanUp,
                                     LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase) {
        this.internalAuthDataCleanUp = internalAuthDataCleanUp;
        this.internalAuthDataCleanUp.setDataModel(InternalAuthDataModel.class);
        this.internalAuthDataCleanUp.setRepository(internalAuthRepository);

        this.personPIIDataCleanUp = personPIIDataCleanUp;
        this.personPIIDataCleanUp.setDataModel(PersonPIIDataModel.class);
        this.personPIIDataCleanUp.setRepository(personPIIRepository);

        this.loadEmployeeMockDataUseCase = loadEmployeeMockDataUseCase;
    }

    public void load() {
        cleanUp();
        loadEmployeeMockDataUseCase.load(50);

    }

    public void cleanUp() {
        loadEmployeeMockDataUseCase.clean();
        internalAuthDataCleanUp.clean();
        personPIIDataCleanUp.clean();
    }
}
