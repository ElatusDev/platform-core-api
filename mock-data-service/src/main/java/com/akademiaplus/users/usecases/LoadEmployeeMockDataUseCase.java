/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.AbstractLoadMockData;
import com.akademiaplus.util.DataCleanUp;
import com.akademiaplus.util.DataLoader;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationRequestDTO;
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