package com.makani.employee.usecases;

import com.makani.PersonPIIDataModel;
import com.makani.employee.interfaceadapters.EmployeeRepository;
import com.makani.interfaceadapters.PersonPIIRepository;
import com.makani.internal.interfaceadapters.InternalAuthRepository;
import com.makani.people.employee.EmployeeDataModel;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.utilities.BatchProcessing;
import openapi.makani.domain.people.dto.EmployeeCreationRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeCreationBatchUseCase implements BatchProcessing<EmployeeCreationRequestDTO> {
    private final EmployeeRepository employeeRepository;
    private final InternalAuthRepository internalAuthRepository;
    private final PersonPIIRepository personPIIRepository;
    private final EmployeeCreationUseCase employeeCreationUseCase;

    public EmployeeCreationBatchUseCase(EmployeeRepository employeeRepository,
                                        EmployeeCreationUseCase employeeCreationUseCase,
                                        InternalAuthRepository internalAuthRepository,
                                        PersonPIIRepository personPIIRepository) {
        this.employeeRepository = employeeRepository;
        this.internalAuthRepository = internalAuthRepository;
        this.personPIIRepository = personPIIRepository;
        this.employeeCreationUseCase = employeeCreationUseCase;
    }

    @Override
    public void createAll(List<EmployeeCreationRequestDTO> dtos) {
        List<EmployeeDataModel> employeeDataModels = dtos.stream().map(employeeCreationUseCase::transform).toList();
        employeeRepository.saveAll(employeeDataModels);
    }
}
