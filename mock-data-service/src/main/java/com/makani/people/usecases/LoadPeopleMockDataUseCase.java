package com.makani.people.usecases;

import com.makani.interfaceadapters.PersonPIIRepository;
import com.makani.internal.interfaceadapters.InternalAuthRepository;
import org.springframework.stereotype.Service;

@Service
public class LoadPeopleMockDataUseCase {
    private final InternalAuthRepository internalAuthRepository;
    private final PersonPIIRepository personPIIRepository;
    private final LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase;
    private final LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase;

    public LoadPeopleMockDataUseCase(InternalAuthRepository internalAuthRepository,
                                     PersonPIIRepository personPIIRepository,
                                     LoadEmployeeMockDataUseCase loadEmployeeMockDataUseCase,
                                     LoadCollaboratorMockDataUseCase loadCollaboratorMockDataUseCase) {
        this.internalAuthRepository = internalAuthRepository;
        this.personPIIRepository = personPIIRepository;
        this.loadEmployeeMockDataUseCase = loadEmployeeMockDataUseCase;
        this.loadCollaboratorMockDataUseCase = loadCollaboratorMockDataUseCase;
    }

    public void load() {
        cleanUp();
        loadEmployeeMockDataUseCase.load();
        loadCollaboratorMockDataUseCase.load();
    }

    private void cleanUp() {
        loadEmployeeMockDataUseCase.clean();
        loadCollaboratorMockDataUseCase.clean();

        internalAuthRepository.deleteAllInBatch();
        internalAuthRepository.flush();

        personPIIRepository.deleteAllInBatch();
        personPIIRepository.flush();

        internalAuthRepository.restIdCounter();
        personPIIRepository.restIdCounter();
    }
}
