/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.people.usecases;

import com.makani.employee.interfaceadapters.EmployeeRepository;
import com.makani.employee.usecases.EmployeeCreationUseCase;
import com.makani.people.employee.EmployeeDataModel;
import com.makani.util.AbstractLoadMockData;
import com.makani.util.DataCleanUp;
import com.makani.util.DataLoader;
import openapi.makani.domain.people.dto.EmployeeCreationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoadEmployeeMockDataUseCase extends AbstractLoadMockData<EmployeeCreationRequestDTO, EmployeeDataModel, Integer> {

    public LoadEmployeeMockDataUseCase(@Value("${employee.mock.data.location}") String employeeMockDataLocation,
                                       EmployeeCreationUseCase employeeCreationUseCase,
                                       EmployeeRepository employeeRepository,
                                       DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Integer> dataLoader,
                                       DataCleanUp<EmployeeDataModel, Integer> dataCleanup) {
        super(employeeMockDataLocation, employeeCreationUseCase::transform, EmployeeCreationRequestDTO.class,
                employeeRepository, EmployeeDataModel.class, dataLoader, dataCleanup);
    }
}