package com.makani.collaborator.usecases;

import com.makani.PersonPIIDataModel;
import com.makani.collaborator.interfaceadapters.CollaboratorRepository;
import com.makani.interfaceadapters.PersonPIIRepository;
import com.makani.internal.interfaceadapters.InternalAuthRepository;
import com.makani.people.collaborator.CollaboratorDataModel;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.utilities.BatchProcessing;
import openapi.makani.domain.people.dto.CollaboratorCreationRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollaboratorCreationBatchUseCase implements BatchProcessing<CollaboratorCreationRequestDTO> {
    private final CollaboratorRepository collaboratorRepository;
    private final InternalAuthRepository internalAuthRepository;
    private final PersonPIIRepository personPIIRepository;
    private final CollaboratorCreationUseCase collaboratorCreationUseCase;

    public CollaboratorCreationBatchUseCase(CollaboratorRepository collaboratorRepository,
                                            InternalAuthRepository internalAuthRepository,
                                            PersonPIIRepository personPIIRepository,
                                            CollaboratorCreationUseCase collaboratorCreationUseCase) {
        this.collaboratorRepository = collaboratorRepository;
        this.internalAuthRepository = internalAuthRepository;
        this.personPIIRepository = personPIIRepository;
        this.collaboratorCreationUseCase = collaboratorCreationUseCase;
    }

    @Override
    public void createAll(List<CollaboratorCreationRequestDTO> dtos) {
        List<CollaboratorDataModel> collaboratorDataModels = dtos.stream().map(collaboratorCreationUseCase::transform).toList();
        collaboratorRepository.saveAll(collaboratorDataModels);
    }
}
