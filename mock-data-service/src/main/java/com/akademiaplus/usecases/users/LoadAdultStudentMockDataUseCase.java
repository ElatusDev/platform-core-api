/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.users;

import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadAdultStudentMockDataUseCase extends AbstractMockDataUseCase<AdultStudentCreationRequestDTO, AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> {

    public LoadAdultStudentMockDataUseCase(DataLoader<AdultStudentCreationRequestDTO, AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> dataLoader,
                                           @Qualifier("adultStudentDataCleanUp") DataCleanUp<AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
