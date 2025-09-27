/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.adultstudent.usecases.AdultStudentCreationUseCase;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.util.AbstractLoadMockData;
import com.akademiaplus.util.DataCleanUp;
import com.akademiaplus.util.DataLoader;
import openapi.akademiaplus.domain.user_management.dto.AdultStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoadAdultStudentMockDataUseCase extends AbstractLoadMockData<AdultStudentCreationRequestDTO, AdultStudentDataModel, Integer> {

    public LoadAdultStudentMockDataUseCase(@Value("${adult.student.mock.data.location}") String adultStudentMockDataLocation,
                                           AdultStudentCreationUseCase adultStudentCreationUseCase,
                                           AdultStudentRepository adultStudentRepository,
                                           DataLoader<AdultStudentCreationRequestDTO, AdultStudentDataModel, Integer> dataLoader,
                                           DataCleanUp<AdultStudentDataModel, Integer> dataCleanUp) {
        super(adultStudentMockDataLocation, adultStudentCreationUseCase::transform, AdultStudentCreationRequestDTO.class,
                adultStudentRepository, AdultStudentDataModel.class, dataLoader, dataCleanUp);
    }

}
