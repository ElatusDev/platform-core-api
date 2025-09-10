package com.makani.people.usecases;

import com.makani.PersonPIIDataModel;
import com.makani.interfaceadapters.PersonPIIRepository;
import com.makani.internal.interfaceadapters.InternalAuthRepository;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.util.DataCleanUp;
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
        loadEmployeeMockDataUseCase.load();
        loadCollaboratorMockDataUseCase.load();
        loadAdultStudentMockDataUseCase.load();
    }

    private void cleanUp() {
        loadEmployeeMockDataUseCase.clean();
        loadCollaboratorMockDataUseCase.clean();
        internalAuthDataCleanUp.clean();
        personPIIDataCleanUp.clean();
    }
}
