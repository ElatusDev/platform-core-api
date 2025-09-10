package com.makani.people.usecases;

import com.makani.collaborator.interfaceadapters.CollaboratorRepository;
import com.makani.collaborator.usecases.CollaboratorCreationUseCase;
import com.makani.people.collaborator.CollaboratorDataModel;
import com.makani.util.AbstractLoadMockData;
import com.makani.util.DataCleanUp;
import com.makani.util.DataLoader;

import openapi.makani.domain.people.dto.CollaboratorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoadCollaboratorMockDataUseCase extends AbstractLoadMockData<CollaboratorCreationRequestDTO, CollaboratorDataModel, Integer> {

    public LoadCollaboratorMockDataUseCase(@Value("${collaborator.mock.data.location}") String collaboratorMockDataLocation,
                                           CollaboratorCreationUseCase collaboratorCreationUseCase,
                                           CollaboratorRepository collaboratorRepository,
                                           DataLoader<CollaboratorCreationRequestDTO, CollaboratorDataModel, Integer> dataLoader,
                                           DataCleanUp<CollaboratorDataModel, Integer> dataCleanUp) {
       super(collaboratorMockDataLocation, collaboratorCreationUseCase::transform, CollaboratorCreationRequestDTO.class,
               collaboratorRepository, CollaboratorDataModel.class, dataLoader, dataCleanUp);
    }
}
