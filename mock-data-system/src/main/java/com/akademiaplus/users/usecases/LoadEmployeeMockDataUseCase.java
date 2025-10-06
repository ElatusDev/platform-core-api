/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.usecases;

import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadEmployeeMockDataUseCase extends AbstractMockDataUseCase<EmployeeCreationRequestDTO, EmployeeDataModel, Integer> {

    public LoadEmployeeMockDataUseCase(DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Integer> dataLoader,
                                       @Qualifier("employeeDataCleanUp") DataCleanUp<EmployeeDataModel, Integer> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}