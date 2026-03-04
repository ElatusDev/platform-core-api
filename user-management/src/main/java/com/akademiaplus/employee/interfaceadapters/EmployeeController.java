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
import com.akademiaplus.employee.usecases.EmployeeUpdateUseCase;
import com.akademiaplus.employee.usecases.GetAllEmployeesUseCase;
import com.akademiaplus.employee.usecases.GetEmployeeByIdUseCase;
import openapi.akademiaplus.domain.user.management.api.EmployeesApi;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeUpdateResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetAllEmployees200ResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetEmployeeResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/user-management")
public class EmployeeController implements EmployeesApi {

    private final GetEmployeeByIdUseCase getEmployeeByIdUseCase;
    private final GetAllEmployeesUseCase getAllEmployeesUseCase;
    private final EmployeeCreationUseCase employeeCreationUseCase;
    private final EmployeeUpdateUseCase employeeUpdateUseCase;
    private final DeleteEmployeeUseCase deleteEmployeeUseCase;

    public EmployeeController(GetEmployeeByIdUseCase getEmployeeByIdUseCase,
                              GetAllEmployeesUseCase getAllEmployeesUseCase,
                              EmployeeCreationUseCase employeeCreationUseCase,
                              EmployeeUpdateUseCase employeeUpdateUseCase,
                              DeleteEmployeeUseCase deleteEmployeeUseCase) {
        this.getEmployeeByIdUseCase = getEmployeeByIdUseCase;
        this.getAllEmployeesUseCase = getAllEmployeesUseCase;
        this.employeeCreationUseCase = employeeCreationUseCase;
        this.employeeUpdateUseCase = employeeUpdateUseCase;
        this.deleteEmployeeUseCase = deleteEmployeeUseCase;
    }

    @Override
    public ResponseEntity<GetEmployeeResponseDTO> getEmployee(Long employeeId) {
        return ResponseEntity.ok(getEmployeeByIdUseCase.get(employeeId));
    }

    @Override
    public ResponseEntity<EmployeeCreationResponseDTO> createEmployee(
            EmployeeCreationRequestDTO employeeCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeCreationUseCase.create(employeeCreationRequestDTO));
    }

    @Override
    public ResponseEntity<EmployeeUpdateResponseDTO> updateEmployee(
            Long employeeId, EmployeeUpdateRequestDTO employeeUpdateRequestDTO) {
        return ResponseEntity.ok(employeeUpdateUseCase.update(employeeId, employeeUpdateRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteEmployee(Long employeeId) {
         deleteEmployeeUseCase.delete(employeeId);
         return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GetAllEmployees200ResponseDTO> getAllEmployees(
            Integer page, Integer size, String employeeType,
            LocalDate entryDateFrom, LocalDate entryDateTo) {
        List<GetEmployeeResponseDTO> employees = getAllEmployeesUseCase.getAll();
        GetAllEmployees200ResponseDTO response = new GetAllEmployees200ResponseDTO();
        response.setData(employees);
        return ResponseEntity.ok(response);
    }
}
