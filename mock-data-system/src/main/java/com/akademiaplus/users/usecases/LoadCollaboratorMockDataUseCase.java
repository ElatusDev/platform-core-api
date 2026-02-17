/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadCollaboratorMockDataUseCase extends AbstractMockDataUseCase<CollaboratorCreationRequestDTO, CollaboratorDataModel, Long> {

    public LoadCollaboratorMockDataUseCase(DataLoader<CollaboratorCreationRequestDTO, CollaboratorDataModel, Long> dataLoader,
                                           @Qualifier("collaboratorDataCleanUp") DataCleanUp<CollaboratorDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
