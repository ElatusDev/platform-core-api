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
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import openapi.akademiaplus.domain.user.management.dto.GetEmployeeResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetEmployeeByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final EmployeeRepository employeeRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetEmployeeByIdUseCase(EmployeeRepository employeeRepository,
                                   TenantContextHolder tenantContextHolder,
                                   ModelMapper modelMapper) {
        this.employeeRepository = employeeRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    public GetEmployeeResponseDTO get(Long employeeId) {
          Long tenantId = tenantContextHolder.getTenantId()
                  .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
          Optional<EmployeeDataModel> queryResult = employeeRepository.findById(
                  new EmployeeDataModel.EmployeeCompositeId(tenantId, employeeId));
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
