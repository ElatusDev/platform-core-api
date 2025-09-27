/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.collaborator.usecases.CollaboratorCreationUseCase;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.util.AbstractLoadMockData;
import com.akademiaplus.util.DataCleanUp;
import com.akademiaplus.util.DataLoader;

import openapi.akademiaplus.domain.user_management.dto.CollaboratorCreationRequestDTO;
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
