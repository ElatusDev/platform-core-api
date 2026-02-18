/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadTutorMockDataUseCase extends AbstractMockDataUseCase<TutorCreationRequestDTO, TutorDataModel, Long> {

    public LoadTutorMockDataUseCase(DataLoader<TutorCreationRequestDTO, TutorDataModel, Long> dataLoader,
                                    @Qualifier("tutorDataCleanUp") DataCleanUp<TutorDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
