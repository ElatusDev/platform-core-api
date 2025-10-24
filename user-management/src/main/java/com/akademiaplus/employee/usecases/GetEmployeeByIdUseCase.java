/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.exception.EmployeeNotFoundException;
import openapi.akademiaplus.domain.user.management.dto.GetEmployeeResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetEmployeeByIdUseCase {
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    public GetEmployeeByIdUseCase(EmployeeRepository employeeRepository, ModelMapper modelMapper) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
    }

    public GetEmployeeResponseDTO get(Long employeeId) {
          Optional<EmployeeDataModel> queryResult = employeeRepository.findById(employeeId);
          if(queryResult.isPresent()) {
              EmployeeDataModel found = queryResult.get();
              GetEmployeeResponseDTO dto = modelMapper.map(found, GetEmployeeResponseDTO.class);
              modelMapper.map(found.getPersonPII() , dto);
              return dto;
          } else {
              throw new EmployeeNotFoundException(String.valueOf(employeeId));
          }
    }
}
