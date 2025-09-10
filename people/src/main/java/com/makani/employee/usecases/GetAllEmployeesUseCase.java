/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.employee.usecases;

import com.makani.employee.interfaceadapters.EmployeeRepository;
import openapi.makani.domain.people.dto.GetEmployeeResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetAllEmployeesUseCase {
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    public GetAllEmployeesUseCase(EmployeeRepository employeeRepository, ModelMapper modelMapper) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
    }

    public List<GetEmployeeResponseDTO> getAll() {
       return employeeRepository.findAll().stream()
                .map(dataModel -> {
                    GetEmployeeResponseDTO dto = modelMapper.map(dataModel.getPersonPII(), GetEmployeeResponseDTO.class);
                     modelMapper.map(dataModel, dto);
                    return dto;
                })
                .toList();
    }
}