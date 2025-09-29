/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.interfaceadapters;

import com.akademiaplus.employee.usecases.DeleteEmployeeUseCase;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.employee.usecases.GetAllEmployeesUseCase;
import com.akademiaplus.employee.usecases.GetEmployeeByIdUseCase;
import openapi.akademiaplus.domain.user_management.api.EmployeesApi;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationResponseDTO;
import openapi.akademiaplus.domain.user_management.dto.GetEmployeeResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/user-management")
public class EmployeeController implements EmployeesApi {

    private final GetEmployeeByIdUseCase getEmployeeByIdUseCase;
    private final GetAllEmployeesUseCase getAllEmployeesUseCase;
    private final EmployeeCreationUseCase employeeCreationUseCase;
    private final DeleteEmployeeUseCase deleteEmployeeUseCase;

    public EmployeeController(GetEmployeeByIdUseCase getEmployeeByIdUseCase,
                              GetAllEmployeesUseCase getAllEmployeesUseCase,
                              EmployeeCreationUseCase employeeCreationUseCase,
                              DeleteEmployeeUseCase deleteEmployeeUseCase) {
        this.getEmployeeByIdUseCase = getEmployeeByIdUseCase;
        this.getAllEmployeesUseCase = getAllEmployeesUseCase;
        this.employeeCreationUseCase = employeeCreationUseCase;
        this.deleteEmployeeUseCase = deleteEmployeeUseCase;
    }

    @Override
    public ResponseEntity<GetEmployeeResponseDTO> getEmployee(Integer employeeId) {
        return ResponseEntity.ok(getEmployeeByIdUseCase.get(employeeId));
    }

    @Override
    public ResponseEntity<EmployeeCreationResponseDTO> createEmployee(
            EmployeeCreationRequestDTO employeeCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeCreationUseCase.create(employeeCreationRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteEmployee(Integer employeeId) {
         deleteEmployeeUseCase.delete(employeeId);
         return ResponseEntity.noContent().build();
    }
}
