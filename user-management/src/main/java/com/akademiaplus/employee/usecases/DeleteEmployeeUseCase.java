/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.exception.EmployeeDeletionNotAllowedException;
import com.akademiaplus.exception.EmployeeNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteEmployeeUseCase {
    private final EmployeeRepository employeeRepository;
    public DeleteEmployeeUseCase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public void delete(Long employeeId) {
        Optional<EmployeeDataModel> found = employeeRepository.findById(employeeId);
        if(found.isPresent()) {
            try {
                employeeRepository.delete(found.get());
            } catch(DataIntegrityViolationException ex) {
                throw new EmployeeDeletionNotAllowedException(ex);
            }
        } else {
            throw new EmployeeNotFoundException(String.valueOf(employeeId));
        }
    }
}
