/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.users;

import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadCollaboratorMockDataUseCase extends AbstractMockDataUseCase<CollaboratorCreationRequestDTO, CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> {

    public LoadCollaboratorMockDataUseCase(DataLoader<CollaboratorCreationRequestDTO, CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> dataLoader,
                                           @Qualifier("collaboratorDataCleanUp") DataCleanUp<CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
