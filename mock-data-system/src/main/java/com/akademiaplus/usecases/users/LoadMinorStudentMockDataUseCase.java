/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.users;

import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadMinorStudentMockDataUseCase extends AbstractMockDataUseCase<MinorStudentCreationRequestDTO, MinorStudentDataModel, Long> {

    public LoadMinorStudentMockDataUseCase(DataLoader<MinorStudentCreationRequestDTO, MinorStudentDataModel, Long> dataLoader,
                                           @Qualifier("minorStudentDataCleanUp") DataCleanUp<MinorStudentDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
