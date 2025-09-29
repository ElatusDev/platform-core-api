/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeCreationUseCase {
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    public static final String MAP_NAME = "employeeMap";

    public EmployeeCreationUseCase(EmployeeRepository employeeRepository,
                                   ModelMapper modelMapper,
                                   HashingService hashingService,
                                   PiiNormalizer piiNormalizer) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
    }

    @Transactional
    public EmployeeCreationResponseDTO create(EmployeeCreationRequestDTO dto)  {
        return modelMapper.map(employeeRepository.save(transform(dto)), EmployeeCreationResponseDTO.class);
    }

    public EmployeeDataModel transform(EmployeeCreationRequestDTO dto) {
        final InternalAuthDataModel internalAuthDataModel= modelMapper.map(dto, InternalAuthDataModel.class);
        final PersonPIIDataModel personPIIDataModel = modelMapper.map(dto, PersonPIIDataModel.class);
        final EmployeeDataModel model =  modelMapper.map(dto, EmployeeDataModel.class, MAP_NAME);
        model.setPersonPII(personPIIDataModel);
        model.setInternalAuth(internalAuthDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhone());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        internalAuthDataModel.setUsernameHash(hashingService.generateHash(internalAuthDataModel.getUsername()));
        return model;
    }


}
